package com.example.shopeeclone.ui.screens.orders

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

class OrderHistoryViewModel(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val orders = mutableStateOf<List<Order>>(emptyList())
    val isLoading = mutableStateOf(true)
    val updatingOrderId = mutableStateOf<String?>(null)

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            val userId = authRepository.currentUserId ?: "guest"
            orders.value = orderRepository.getOrdersForUser(userId)
            isLoading.value = false
        }
    }

    fun updateStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            updatingOrderId.value = orderId
            orderRepository.updateOrderStatus(orderId, status)
            load()
            updatingOrderId.value = null
        }
    }

    fun requestReturn(orderId: String, reason: String) {
        viewModelScope.launch {
            updatingOrderId.value = orderId
            orderRepository.requestReturn(orderId, reason)
            load()
            updatingOrderId.value = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    viewModel: OrderHistoryViewModel = viewModel()
) {
    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var returnDialogOrderId by remember { mutableStateOf<String?>(null) }
    var returnReasonInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    if (returnDialogOrderId != null) {
        AlertDialog(
            onDismissRequest = { returnDialogOrderId = null },
            title = { Text("Request Return/Refund") },
            text = {
                OutlinedTextField(
                    value = returnReasonInput,
                    onValueChange = { returnReasonInput = it },
                    placeholder = { Text("Tell us what went wrong...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        returnDialogOrderId?.let { viewModel.requestReturn(it, returnReasonInput) }
                        returnDialogOrderId = null
                        returnReasonInput = ""
                    },
                    enabled = returnReasonInput.isNotBlank()
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { returnDialogOrderId = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
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
                        OrderCard(
                            order = order,
                            isUpdating = viewModel.updatingOrderId.value == order.id,
                            onPayNow = { viewModel.updateStatus(order.id, OrderStatus.TO_SHIP) },
                            onCancel = { viewModel.updateStatus(order.id, OrderStatus.CANCELLED) },
                            onConfirmReceipt = { viewModel.updateStatus(order.id, OrderStatus.COMPLETED) },
                            onRequestReturn = {
                                returnDialogOrderId = order.id
                                returnReasonInput = ""
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    isUpdating: Boolean,
    onPayNow: () -> Unit,
    onCancel: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onRequestReturn: () -> Unit
) {
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
        if (order.shippingMethod.isNotBlank()) {
            Text("Shipping: ${order.shippingMethod}", style = MaterialTheme.typography.bodySmall)
        }
        if (order.status == OrderStatus.RETURN_REQUESTED || order.status == OrderStatus.REFUNDED) {
            if (order.returnReason.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Reason: ${order.returnReason}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (order.status) {
                OrderStatus.UNPAID -> {
                    OutlinedButton(onClick = onCancel, enabled = !isUpdating) { Text("Cancel Order") }
                    Button(onClick = onPayNow, enabled = !isUpdating) { Text(if (isUpdating) "..." else "Pay Now") }
                }
                OrderStatus.TO_SHIP -> {
                    OutlinedButton(onClick = onCancel, enabled = !isUpdating) { Text("Cancel Order") }
                }
                OrderStatus.SHIPPED -> {
                    Button(onClick = onConfirmReceipt, enabled = !isUpdating) {
                        Text(if (isUpdating) "..." else "Confirm Receipt")
                    }
                    OutlinedButton(onClick = onRequestReturn, enabled = !isUpdating) { Text("Return/Refund") }
                }
                OrderStatus.COMPLETED -> {
                    OutlinedButton(onClick = onRequestReturn, enabled = !isUpdating) { Text("Return/Refund") }
                }
                OrderStatus.RETURN_REQUESTED, OrderStatus.REFUNDED, OrderStatus.CANCELLED -> {
                    // No further buyer action.
                }
            }
        }
    }
}
