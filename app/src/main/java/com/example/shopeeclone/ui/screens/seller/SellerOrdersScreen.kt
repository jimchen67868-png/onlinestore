package com.example.shopeeclone.ui.screens.seller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.shopeeclone.data.model.Order
import com.example.shopeeclone.data.model.OrderStatus
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.OrderRepository
import kotlinx.coroutines.launch

class SellerOrdersViewModel(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val orders = mutableStateOf<List<Order>>(emptyList())
    val isLoading = mutableStateOf(true)
    val updatingOrderId = mutableStateOf<String?>(null)

    private val sellerId: String
        get() = authRepository.currentUserId ?: ""

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            val visible = orderRepository.getAllVisibleOrders()
            orders.value = visible.filter { order -> order.items.any { it.product.sellerId == sellerId } }
            isLoading.value = false
        }
    }

    fun markAsShipped(orderId: String) {
        viewModelScope.launch {
            updatingOrderId.value = orderId
            orderRepository.updateOrderStatus(orderId, OrderStatus.SHIPPED)
            load()
            updatingOrderId.value = null
        }
    }

    fun approveReturn(orderId: String) {
        viewModelScope.launch {
            updatingOrderId.value = orderId
            orderRepository.updateOrderStatus(orderId, OrderStatus.REFUNDED)
            load()
            updatingOrderId.value = null
        }
    }

    fun rejectReturn(orderId: String) {
        viewModelScope.launch {
            updatingOrderId.value = orderId
            orderRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED)
            load()
            updatingOrderId.value = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersScreen(
    onBack: () -> Unit,
    viewModel: SellerOrdersViewModel = viewModel()
) {
    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All") }
                    )
                }
                items(OrderStatus.entries.toList()) { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        label = { Text(status.label) }
                    )
                }
            }

            val filteredOrders = if (selectedFilter == null) viewModel.orders.value
            else viewModel.orders.value.filter { it.status == selectedFilter }

            when {
                viewModel.isLoading.value -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                filteredOrders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders here yet")
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredOrders, key = { it.id }) { order ->
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Order #${order.id.take(8)}", fontWeight = FontWeight.Bold)
                                Text(order.status.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(4.dp))
                            order.items.forEach {
                                Text("${it.product.name} × ${it.quantity}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Total: $${"%.2f".format(order.totalAmount)}")
                            if (order.status == OrderStatus.RETURN_REQUESTED && order.returnReason.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Reason: ${order.returnReason}", style = MaterialTheme.typography.bodySmall)
                            }

                            if (order.status == OrderStatus.TO_SHIP) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.markAsShipped(order.id) },
                                    enabled = viewModel.updatingOrderId.value != order.id
                                ) {
                                    Text(if (viewModel.updatingOrderId.value == order.id) "..." else "Mark as Shipped")
                                }
                            }
                            if (order.status == OrderStatus.RETURN_REQUESTED) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.rejectReturn(order.id) },
                                        enabled = viewModel.updatingOrderId.value != order.id
                                    ) { Text("Reject") }
                                    Button(
                                        onClick = { viewModel.approveReturn(order.id) },
                                        enabled = viewModel.updatingOrderId.value != order.id
                                    ) { Text(if (viewModel.updatingOrderId.value == order.id) "..." else "Approve Refund") }
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
