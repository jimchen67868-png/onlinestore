package com.example.shopeeclone.ui.screens.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onOrderPlaced: () -> Unit,
    onBack: () -> Unit,
    viewModel: CartViewModel = viewModel()
) {
    var address by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash on Delivery") }
    var voucherInput by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.orderPlacedSuccessfully.value) {
        if (viewModel.orderPlacedSuccessfully.value) onOrderPlaced()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Shipping Address", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("Enter full address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Payment Method", fontWeight = FontWeight.Bold)
            listOf("Cash on Delivery", "Credit / Debit Card", "E-Wallet").forEach { method ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = paymentMethod == method, onClick = { paymentMethod = method })
                    Text(method)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Voucher", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val voucher = viewModel.appliedVoucher.value
            if (voucher != null) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("\"${voucher.code}\" applied — you saved $${"%.2f".format(viewModel.discountAmount())}", color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.removeVoucher(); voucherInput = "" }) { Text("Remove") }
                }
            } else {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = voucherInput,
                        onValueChange = { voucherInput = it },
                        placeholder = { Text("Enter voucher code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.applyVoucher(voucherInput) },
                        enabled = voucherInput.isNotBlank() && !viewModel.isValidatingVoucher.value
                    ) {
                        Text(if (viewModel.isValidatingVoucher.value) "..." else "Apply")
                    }
                }
                viewModel.voucherErrorMessage.value?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Order Summary", fontWeight = FontWeight.Bold)
            viewModel.items.forEach {
                Text("${it.product.name} × ${it.quantity} — $${"%.2f".format(it.subtotal)}")
            }
            Spacer(Modifier.height(8.dp))
            Text("Subtotal: $${"%.2f".format(viewModel.total())}")
            if (viewModel.discountAmount() > 0) {
                Text("Voucher discount: -$${"%.2f".format(viewModel.discountAmount())}", color = MaterialTheme.colorScheme.primary)
            }
            Text("Total: $${"%.2f".format(viewModel.finalTotal())}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { viewModel.placeOrder(address) },
                enabled = address.isNotBlank() && !viewModel.isPlacingOrder.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isPlacingOrder.value) "Placing order..." else "Place Order")
            }
        }
    }
}
