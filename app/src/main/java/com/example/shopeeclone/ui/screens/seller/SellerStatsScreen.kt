package com.example.shopeeclone.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.shopeeclone.data.repository.OrderRepository
import kotlinx.coroutines.launch

data class SellerStats(
    val totalRevenue: Double = 0.0,
    val orderCount: Int = 0,
    val itemsSold: Int = 0,
    val topProducts: List<Pair<String, Int>> = emptyList() // name, quantity sold
)

class SellerStatsViewModel(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val isLoading = mutableStateOf(true)
    val stats = mutableStateOf(SellerStats())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            val sellerId = authRepository.currentUserId ?: ""
            val visibleOrders = orderRepository.getAllVisibleOrders()

            var revenue = 0.0
            var itemsSold = 0
            var orderCount = 0
            val productSales = mutableMapOf<String, Int>()

            visibleOrders.forEach { order ->
                val myItems = order.items.filter { it.product.sellerId == sellerId }
                if (myItems.isNotEmpty()) {
                    orderCount++
                    myItems.forEach { item ->
                        revenue += item.subtotal
                        itemsSold += item.quantity
                        productSales[item.product.name] = (productSales[item.product.name] ?: 0) + item.quantity
                    }
                }
            }

            val topProducts = productSales.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }

            stats.value = SellerStats(revenue, orderCount, itemsSold, topProducts)
            isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerStatsScreen(
    onBack: () -> Unit,
    viewModel: SellerStatsViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Stats") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val stats = viewModel.stats.value
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            label = "Total Revenue",
                            value = "$${"%.2f".format(stats.totalRevenue)}",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        StatCard(
                            label = "Orders",
                            value = "${stats.orderCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    StatCard(
                        label = "Items Sold",
                        value = "${stats.itemsSold}",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Text("Top Products", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }

                if (stats.topProducts.isEmpty()) {
                    item { Text("No sales yet.", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(stats.topProducts) { (name, qty) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontWeight = FontWeight.Medium)
                            Text("$qty sold", color = MaterialTheme.colorScheme.primary)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
