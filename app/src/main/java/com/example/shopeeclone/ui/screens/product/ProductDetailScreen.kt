package com.example.shopeeclone.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.FollowRepository
import com.example.shopeeclone.viewmodel.CartViewModel
import com.example.shopeeclone.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onGoToCart: () -> Unit,
    onVisitShop: (String, String) -> Unit,
    productViewModel: ProductViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    followRepository: FollowRepository = FollowRepository()
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf(1) }
    var isFetching by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var isTogglingFollow by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(productId) {
        isFetching = true
        product = productViewModel.getProduct(productId)
        isFetching = false
        product?.let { isFollowing = followRepository.isFollowing(it.sellerId) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        product?.let { p ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (p.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = p.imageUrl,
                        contentDescription = p.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.background)
                    )
                }

                if (p.videoUrl.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        factory = { ctx ->
                            android.widget.VideoView(ctx).apply {
                                setVideoURI(android.net.Uri.parse(p.videoUrl))
                                setMediaController(android.widget.MediaController(ctx).also { it.setAnchorView(this) })
                                setOnPreparedListener { it.isLooping = false }
                                start()
                            }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$${p.discountPrice ?: p.price}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (p.discountPrice != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$${p.price}",
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onVisitShop(p.sellerId, p.sellerName) }
                ) {
                    Text(
                        "${p.soldCount} sold · ★${p.rating} · Sold by ${p.sellerName}",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline)
                    )
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        isTogglingFollow = true
                        coroutineScope.launch {
                            if (isFollowing) {
                                followRepository.unfollow(p.sellerId).onSuccess { isFollowing = false }
                            } else {
                                followRepository.follow(p.sellerId, p.sellerName).onSuccess { isFollowing = true }
                            }
                            isTogglingFollow = false
                        }
                    },
                    enabled = !isTogglingFollow
                ) {
                    Text(if (isFollowing) "Following" else "+ Follow Shop")
                }
                Spacer(Modifier.height(16.dp))
                Text("Description", fontWeight = FontWeight.Bold)
                Text(p.description, style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantity: ")
                    IconButton(onClick = { if (quantity > 1) quantity-- }) { Text("-") }
                    Text("$quantity")
                    IconButton(onClick = { if (quantity < p.stock) quantity++ }) { Text("+") }
                }

                Spacer(Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            cartViewModel.addToCart(p, quantity)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Added to cart")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add to Cart") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            cartViewModel.addToCart(p, quantity)
                            onGoToCart()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Buy Now") }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (isFetching) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load this product.", fontWeight = FontWeight.Bold)
                    productViewModel.errorMessage.value?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
        }
    }
}
