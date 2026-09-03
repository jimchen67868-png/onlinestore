package com.example.shopeeclone.ui.screens.wishlist

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.LikeRepository
import com.example.shopeeclone.data.repository.ProductRepository
import com.example.shopeeclone.ui.screens.home.ProductCard
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val likeRepository: LikeRepository = LikeRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {
    val products = mutableStateOf<List<Product>>(emptyList())
    val isLoading = mutableStateOf(true)

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            val likedIds = likeRepository.getMyLikedProductIds().toSet()
            products.value = if (likedIds.isEmpty()) {
                emptyList()
            } else {
                productRepository.getProducts().filter { it.id in likedIds }
            }
            isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: WishlistViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Likes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading.value -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.products.value.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("You haven't liked any products yet.")
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(viewModel.products.value, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onProductClick(product.id) })
                }
            }
        }
    }
}
