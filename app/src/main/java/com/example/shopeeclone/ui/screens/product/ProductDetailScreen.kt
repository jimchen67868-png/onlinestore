package com.example.shopeeclone.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.viewmodel.CartViewModel
import com.example.shopeeclone.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onGoToCart: () -> Unit,
    productViewModel: ProductViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel()
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf(1) }
    var isFetching by remember { mutableStateOf(true) }

    LaunchedEffect(productId) {
        isFetching = true
        product = productViewModel.getProduct(productId)
        isFetching = false
    }

    Scaffold(
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.background)
                )
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
                Text("${p.soldCount} sold · ★${p.rating} · Sold by ${p.sellerName}", style = MaterialTheme.typography.bodySmall)
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
