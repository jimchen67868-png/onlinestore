package com.example.shopeeclone.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shopeeclone.data.model.Order
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.OrderRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    orderRepository: OrderRepository = OrderRepository(),
    authRepository: AuthRepository = AuthRepository()
) {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = authRepository.currentUserId ?: "guest"
        orders = orderRepository.getOrdersForUser(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            orders.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No orders yet")
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(orders) { order ->
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Order #${order.id.take(8)}", fontWeight = FontWeight.Bold)
                        Text("Status: ${order.status}")
                        Text("Items: ${order.items.size} · Total: $${"%.2f".format(order.totalAmount)}")
                        if (order.shippingMethod.isNotBlank()) {
                            Text("Shipping: ${order.shippingMethod}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
