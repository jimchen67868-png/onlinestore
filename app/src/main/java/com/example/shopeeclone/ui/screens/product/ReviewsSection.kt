package com.example.shopeeclone.ui.screens.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Review
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.ReviewRepository
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val reviewRepository: ReviewRepository = ReviewRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val reviews = mutableStateOf<List<Review>>(emptyList())
    val myReview = mutableStateOf<Review?>(null)
    val isLoading = mutableStateOf(true)
    val isSubmitting = mutableStateOf(false)
    val statusMessage = mutableStateOf<String?>(null)

    fun load(productId: String) {
        viewModelScope.launch {
            isLoading.value = true
            reviews.value = reviewRepository.getReviewsForProduct(productId)
            val uid = authRepository.currentUserId
            myReview.value = uid?.let { reviewRepository.getMyReview(productId, it) }
            isLoading.value = false
        }
    }

    fun submit(productId: String, rating: Int, comment: String, onDone: () -> Unit) {
        viewModelScope.launch {
            isSubmitting.value = true
            statusMessage.value = null
            val uid = authRepository.currentUserId
            if (uid == null) {
                statusMessage.value = "Please log in to write a review."
                isSubmitting.value = false
                return@launch
            }
            val profile = authRepository.getUserProfile()
            val buyerName = profile?.name?.ifBlank { null } ?: "Buyer"
            val result = reviewRepository.submitReview(productId, uid, buyerName, rating, comment)
            isSubmitting.value = false
            result.onSuccess {
                load(productId)
                onDone()
            }.onFailure {
                statusMessage.value = "Failed: ${it.message}"
            }
        }
    }
}

/** Renders a row of tappable stars. Uses plain text glyphs to avoid any icon-availability risk. */
@Composable
fun StarRatingInput(rating: Int, onRatingChange: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            Text(
                if (i <= rating) "★" else "☆",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onRatingChange(i) }
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun StarRatingDisplay(rating: Double) {
    Row {
        for (i in 1..5) {
            Text(
                if (i <= rating.toInt() || (i - rating) < 1.0 && (i - rating) > 0.0) "★" else "☆",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsSection(
    productId: String,
    viewModel: ReviewViewModel = viewModel()
) {
    var showWriteDialog by remember { mutableStateOf(false) }
    var draftRating by remember { mutableStateOf(5) }
    var draftComment by remember { mutableStateOf("") }

    LaunchedEffect(productId) { viewModel.load(productId) }

    if (showWriteDialog) {
        AlertDialog(
            onDismissRequest = { showWriteDialog = false },
            title = { Text(if (viewModel.myReview.value != null) "Edit Your Review" else "Write a Review") },
            text = {
                Column {
                    StarRatingInput(rating = draftRating, onRatingChange = { draftRating = it })
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draftComment,
                        onValueChange = { draftComment = it },
                        placeholder = { Text("Share your thoughts about this product...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    viewModel.statusMessage.value?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.submit(productId, draftRating, draftComment) {
                            showWriteDialog = false
                        }
                    },
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(if (viewModel.isSubmitting.value) "Submitting..." else "Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reviews (${viewModel.reviews.value.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                val existing = viewModel.myReview.value
                draftRating = existing?.rating ?: 5
                draftComment = existing?.comment ?: ""
                showWriteDialog = true
            }) {
                Text(if (viewModel.myReview.value != null) "Edit Review" else "Write a Review")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (viewModel.reviews.value.isEmpty()) {
            Text("No reviews yet. Be the first to review this product!", style = MaterialTheme.typography.bodyMedium)
        } else {
            viewModel.reviews.value.forEach { review ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(review.buyerName.ifBlank { "Anonymous" }, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        StarRatingDisplay(review.rating.toDouble())
                    }
                    if (review.comment.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Divider()
            }
        }
    }
}
