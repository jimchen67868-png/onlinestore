package com.example.shopeeclone.ui.screens.shop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.FollowRepository
import com.example.shopeeclone.data.repository.ProductRepository
import com.example.shopeeclone.ui.screens.home.ProductCard
import kotlinx.coroutines.launch

class SellerShopViewModel(
    private val productRepository: ProductRepository = ProductRepository(),
    private val followRepository: FollowRepository = FollowRepository()
) : ViewModel() {
    val products = mutableStateOf<List<com.example.shopeeclone.data.model.Product>>(emptyList())
    val isLoading = mutableStateOf(true)
    val isFollowing = mutableStateOf(false)
    val isTogglingFollow = mutableStateOf(false)

    fun load(sellerId: String) {
        viewModelScope.launch {
            isLoading.value = true
            products.value = productRepository.getProductsForSeller(sellerId)
            isFollowing.value = followRepository.isFollowing(sellerId)
            isLoading.value = false
        }
    }

    fun toggleFollow(sellerId: String, sellerName: String) {
        viewModelScope.launch {
            isTogglingFollow.value = true
            if (isFollowing.value) {
                followRepository.unfollow(sellerId).onSuccess { isFollowing.value = false }
            } else {
                followRepository.follow(sellerId, sellerName).onSuccess { isFollowing.value = true }
            }
            isTogglingFollow.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerShopScreen(
    sellerId: String,
    sellerName: String,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onChatWithSeller: (String, String, String, String) -> Unit, // buyerId, buyerName, sellerId, sellerName
    viewModel: SellerShopViewModel = viewModel(),
    authRepository: AuthRepository = AuthRepository()
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(sellerId) { viewModel.load(sellerId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sellerName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sellerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("${viewModel.products.value.size} products", style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { viewModel.toggleFollow(sellerId, sellerName) },
                    enabled = !viewModel.isTogglingFollow.value,
                    colors = if (viewModel.isFollowing.value)
                        ButtonDefaults.outlinedButtonColors()
                    else ButtonDefaults.buttonColors()
                ) {
                    Text(if (viewModel.isFollowing.value) "Following" else "Follow")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val buyerId = authRepository.currentUserId ?: return@launch
                            val profile = authRepository.getUserProfile()
                            val buyerName = profile?.name?.ifBlank { null } ?: "Buyer"
                            onChatWithSeller(buyerId, buyerName, sellerId, sellerName)
                        }
                    }
                ) {
                    Text("Chat")
                }
            }
            Divider()

            if (viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.products.value.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This shop has no products yet.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.products.value, key = { it.id }) { product ->
                        ProductCard(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
        }
    }
}
