package com.example.shopeeclone.ui.screens.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.ProductRepository
import com.example.shopeeclone.ui.screens.home.ProductCard
import kotlinx.coroutines.launch

class RelatedProductsViewModel(
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {
    val moreFromShop = mutableStateOf<List<Product>>(emptyList())
    val youMayAlsoLike = mutableStateOf<List<Product>>(emptyList())
    val isLoading = mutableStateOf(true)

    fun load(currentProductId: String, sellerId: String, category: String) {
        viewModelScope.launch {
            isLoading.value = true
            moreFromShop.value = productRepository.getProductsForSeller(sellerId)
                .filter { it.id != currentProductId }
                .take(10)

            val shopIds = moreFromShop.value.map { it.id }.toSet()
            youMayAlsoLike.value = productRepository.getProducts()
                .filter { it.id != currentProductId && it.category.equals(category, ignoreCase = true) && it.id !in shopIds }
                .take(10)

            isLoading.value = false
        }
    }
}

@Composable
fun RelatedProductsSection(
    productId: String,
    sellerId: String,
    sellerName: String,
    category: String,
    onProductClick: (String) -> Unit,
    onVisitShop: (String, String) -> Unit,
    viewModel: RelatedProductsViewModel = viewModel()
) {
    LaunchedEffect(productId) { viewModel.load(productId, sellerId, category) }

    if (viewModel.isLoading.value) return

    Column(modifier = Modifier.fillMaxWidth()) {
        if (viewModel.moreFromShop.value.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("More from $sellerName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onVisitShop(sellerId, sellerName) }) { Text("View Shop") }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.moreFromShop.value, key = { "shop_" + it.id }) { product ->
                    Box(modifier = Modifier.width(140.dp)) {
                        ProductCard(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (viewModel.youMayAlsoLike.value.isNotEmpty()) {
            Text("You May Also Like", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.youMayAlsoLike.value, key = { "related_" + it.id }) { product ->
                    Box(modifier = Modifier.width(140.dp)) {
                        ProductCard(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
