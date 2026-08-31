package com.example.shopeeclone.ui.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shopeeclone.data.model.CartItem
import com.example.shopeeclone.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        bottomBar = {
            if (viewModel.items.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = viewModel.selectedProductIds.size == viewModel.items.size && viewModel.items.isNotEmpty(),
                            onCheckedChange = { viewModel.toggleSelectAll() }
                        )
                        Text("All")
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total: $${"%.2f".format(viewModel.total())}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onCheckout,
                            enabled = viewModel.selectedProductIds.isNotEmpty()
                        ) {
                            Text("Checkout (${viewModel.selectedProductIds.size})")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (viewModel.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your cart is empty")
            }
        } else {
            val grouped = viewModel.items.groupBy { it.product.sellerName.ifBlank { "Other" } }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                grouped.forEach { (shopName, shopItems) ->
                    item(key = "header_$shopName") {
                        val shopProductIds = shopItems.map { it.product.id }
                        val allShopSelected = shopProductIds.all { it in viewModel.selectedProductIds }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = allShopSelected,
                                onCheckedChange = { viewModel.toggleSelectShop(shopName) }
                            )
                            Text(shopName, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(shopItems, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            isSelected = item.product.id in viewModel.selectedProductIds,
                            onToggleSelected = { viewModel.toggleSelected(item.product.id) },
                            onIncrease = { viewModel.updateQuantity(item.product.id, item.quantity + 1) },
                            onDecrease = { viewModel.updateQuantity(item.product.id, item.quantity - 1) },
                            onRemove = { viewModel.removeFromCart(item.product.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
        if (item.product.imageUrl.isNotBlank()) {
            AsyncImage(
                model = item.product.imageUrl,
                contentDescription = item.product.name,
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.background)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Medium, maxLines = 2)
            Text("$${item.product.discountPrice ?: item.product.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onDecrease) { Text("-") }
        Text("${item.quantity}")
        IconButton(onClick = onIncrease) { Text("+") }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
    }
}
