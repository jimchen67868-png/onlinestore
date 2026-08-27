package com.example.shopeeclone.ui.screens.seller

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Voucher
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.VoucherRepository
import kotlinx.coroutines.launch
import java.util.UUID

class SellerVoucherViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val voucherRepository: VoucherRepository = VoucherRepository()
) : ViewModel() {
    val vouchers = mutableStateOf<List<Voucher>>(emptyList())
    val isLoading = mutableStateOf(true)
    val isSaving = mutableStateOf(false)
    val statusMessage = mutableStateOf<String?>(null)

    private val sellerId: String
        get() = authRepository.currentUserId ?: "guest"

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            vouchers.value = voucherRepository.getSellerVouchers(sellerId)
            isLoading.value = false
        }
    }

    fun createVoucher(
        code: String,
        isPercentage: Boolean,
        discountValue: Double,
        minSpend: Double,
        maxDiscount: Double?,
        usageLimit: Int,
        expiresAt: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            val voucher = Voucher(
                id = UUID.randomUUID().toString(),
                code = code.trim().uppercase(),
                sellerId = sellerId,
                discountType = if (isPercentage) "percentage" else "fixed",
                discountValue = discountValue,
                minSpend = minSpend,
                maxDiscount = maxDiscount,
                usageLimit = usageLimit,
                timesUsed = 0,
                expiresAt = expiresAt
            )
            val result = voucherRepository.createVoucher(voucher)
            isSaving.value = false
            result.onSuccess {
                statusMessage.value = "Voucher created!"
                load()
                onDone()
            }.onFailure {
                statusMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun deleteVoucher(id: String) {
        viewModelScope.launch {
            voucherRepository.deleteVoucher(id).onSuccess { load() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerVoucherScreen(
    onBack: () -> Unit,
    viewModel: SellerVoucherViewModel = viewModel()
) {
    var code by remember { mutableStateOf("") }
    var isPercentage by remember { mutableStateOf(false) }
    var discountValue by remember { mutableStateOf("") }
    var minSpend by remember { mutableStateOf("") }
    var maxDiscount by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("") }
    var expiresAt by remember { mutableStateOf("") }

    fun clearForm() {
        code = ""; discountValue = ""; minSpend = ""; maxDiscount = ""; usageLimit = ""; expiresAt = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Vouchers") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text("Create Voucher", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Voucher Code (e.g. SAVE10)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Discount type:")
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = !isPercentage, onClick = { isPercentage = false }, label = { Text("Fixed $") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = isPercentage, onClick = { isPercentage = true }, label = { Text("Percentage %") })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = discountValue,
                    onValueChange = { discountValue = it },
                    label = { Text(if (isPercentage) "Discount (%)" else "Discount amount ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = minSpend,
                    onValueChange = { minSpend = it },
                    label = { Text("Minimum spend ($, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isPercentage) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxDiscount,
                        onValueChange = { maxDiscount = it },
                        label = { Text("Max discount cap ($, optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = usageLimit,
                    onValueChange = { usageLimit = it },
                    label = { Text("Usage limit (0 = unlimited)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = expiresAt,
                    onValueChange = { expiresAt = it },
                    label = { Text("Expiry date (yyyy-MM-dd, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                viewModel.statusMessage.value?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.createVoucher(
                            code = code,
                            isPercentage = isPercentage,
                            discountValue = discountValue.toDoubleOrNull() ?: 0.0,
                            minSpend = minSpend.toDoubleOrNull() ?: 0.0,
                            maxDiscount = maxDiscount.toDoubleOrNull(),
                            usageLimit = usageLimit.toIntOrNull() ?: 0,
                            expiresAt = expiresAt.ifBlank { null },
                            onDone = { clearForm() }
                        )
                    },
                    enabled = code.isNotBlank() && discountValue.toDoubleOrNull() != null && !viewModel.isSaving.value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.isSaving.value) "Creating..." else "Create Voucher")
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                Text("My Vouchers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (viewModel.isLoading.value) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (viewModel.vouchers.value.isEmpty()) {
                item { Text("No vouchers yet.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(viewModel.vouchers.value, key = { it.id }) { voucher ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(voucher.code, fontWeight = FontWeight.Bold)
                            val discountText = if (voucher.discountType == "percentage")
                                "${voucher.discountValue.toInt()}% off" else "$${voucher.discountValue} off"
                            Text(
                                "$discountText · Used ${voucher.timesUsed}${if (voucher.usageLimit > 0) "/${voucher.usageLimit}" else ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { viewModel.deleteVoucher(voucher.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
