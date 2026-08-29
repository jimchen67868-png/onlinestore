package com.example.shopeeclone.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.FollowRepository
import com.example.shopeeclone.viewmodel.CartViewModel
import com.example.shopeeclone.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

private data class MediaSlide(val type: String, val url: String) // type = "image" or "video"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onGoToCart: () -> Unit,
    onVisitShop: (String, String) -> Unit,
    onChatWithSeller: (String, String, Product) -> Unit, // buyerId, buyerName, product
    productViewModel: ProductViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    followRepository: FollowRepository = FollowRepository(),
    authRepository: AuthRepository = AuthRepository()
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf(1) }
    var isFetching by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var isTogglingFollow by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
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
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                val slides = remember(p.imageUrl, p.videoUrl) {
                    buildList {
                        if (p.imageUrl.isNotBlank()) add(MediaSlide("image", p.imageUrl))
                        if (p.videoUrl.isNotBlank()) add(MediaSlide("video", p.videoUrl))
                    }
                }

                if (slides.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { slides.size })
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            when (slides[page].type) {
                                "image" -> AsyncImage(
                                    model = slides[page].url,
                                    contentDescription = p.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                        .clickable { fullscreenImageUrl = slides[page].url }
                                )
                                "video" -> AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        android.widget.VideoView(ctx).apply {
                                            setVideoURI(android.net.Uri.parse(slides[page].url))
                                            setMediaController(android.widget.MediaController(ctx).also { it.setAnchorView(this) })
                                            setOnPreparedListener { it.isLooping = false }
                                            start()
                                        }
                                    }
                                )
                            }
                        }
                        if (slides.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                slides.indices.forEach { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                if (pagerState.currentPage == index)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
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
                    Row {
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
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val buyerId = authRepository.currentUserId ?: return@launch
                                    val profile = authRepository.getUserProfile()
                                    val buyerName = profile?.name?.ifBlank { null } ?: "Buyer"
                                    onChatWithSeller(buyerId, buyerName, p)
                                }
                            }
                        ) {
                            Text("Chat")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Description", fontWeight = FontWeight.Bold)
                    Text(p.description, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quantity: ")
                        IconButton(onClick = { if (quantity > 1) quantity-- }) { Text("-") }
                        Text("$quantity")
                        IconButton(onClick = {
                            // Treat stock of 0 as "not tracked" rather than "sold out",
                            // so sellers who leave stock blank don't accidentally block purchases.
                            if (p.stock <= 0 || quantity < p.stock) quantity++
                        }) { Text("+") }
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

    fullscreenImageUrl?.let { url ->
        Dialog(
            onDismissRequest = { fullscreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullscreenImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
