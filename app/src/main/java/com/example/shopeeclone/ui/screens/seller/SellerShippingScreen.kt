package com.example.shopeeclone.ui.screens.seller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.shopeeclone.data.model.ProductShippingChannel
import com.example.shopeeclone.data.model.ShippingChannelCatalog
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.ProductRepository
import com.example.shopeeclone.data.repository.ShippingRepository
import kotlinx.coroutines.launch
import java.util.UUID

class SellerShippingViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
    private val shippingRepository: ShippingRepository = ShippingRepository()
) : ViewModel() {
    private val sellerId: String
        get() = authRepository.currentUserId ?: "guest"

    val weight = mutableStateOf("")
    val length = mutableStateOf("")
    val width = mutableStateOf("")
    val height = mutableStateOf("")
    val dangerousGoods = mutableStateOf(false)
    val preOrder = mutableStateOf(false)
    val shipOutDays = mutableStateOf("1")

    // channelKey -> enabled / fee text
    val channelEnabled = mutableStateMapOf<String, Boolean>()
    val channelFee = mutableStateMapOf<String, String>()

    val isLoading = mutableStateOf(true)
    val isSaving = mutableStateOf(false)
    val statusMessage = mutableStateOf<String?>(null)

    fun load(productId: String) {
        viewModelScope.launch {
            isLoading.value = true
            productRepository.getProductById(productId)?.let { product ->
                weight.value = if (product.weightKg > 0) product.weightKg.toString() else ""
                length.value = if (product.lengthCm > 0) product.lengthCm.toString() else ""
                width.value = if (product.widthCm > 0) product.widthCm.toString() else ""
                height.value = if (product.heightCm > 0) product.heightCm.toString() else ""
                dangerousGoods.value = product.dangerousGoods
                preOrder.value = product.preOrder
                shipOutDays.value = product.shipOutDays.toString()
            }
            val existingChannels = shippingRepository.getProductChannels(productId).associateBy { it.channelKey }
            ShippingChannelCatalog.all.forEach { def ->
                val existing = existingChannels[def.key]
                channelEnabled[def.key] = existing?.enabled ?: false
                channelFee[def.key] = existing?.fee?.toString() ?: "0"
            }
            isLoading.value = false
        }
    }

    fun save(productId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            try {
                val existing = productRepository.getProductById(productId)
                    ?: throw IllegalStateException("Product not found")
                val updated = existing.copy(
                    weightKg = weight.value.toDoubleOrNull() ?: 0.0,
                    lengthCm = length.value.toDoubleOrNull() ?: 0.0,
                    widthCm = width.value.toDoubleOrNull() ?: 0.0,
                    heightCm = height.value.toDoubleOrNull() ?: 0.0,
                    dangerousGoods = dangerousGoods.value,
                    preOrder = preOrder.value,
                    shipOutDays = shipOutDays.value.toIntOrNull() ?: 1
                )
                productRepository.updateProduct(updated).getOrThrow()

                ShippingChannelCatalog.all.forEach { def ->
                    shippingRepository.upsertProductChannel(
                        ProductShippingChannel(
                            id = UUID.randomUUID().toString(),
                            productId = productId,
                            sellerId = sellerId,
                            channelKey = def.key,
                            enabled = channelEnabled[def.key] ?: false,
                            fee = channelFee[def.key]?.toDoubleOrNull() ?: 0.0
                        )
                    ).getOrThrow()
                }

                statusMessage.value = "Shipping settings saved!"
                onDone()
            } catch (e: Exception) {
                statusMessage.value = "Failed: ${e.message}"
            } finally {
                isSaving.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerShippingScreen(
    productId: String,
    productName: String,
    onBack: () -> Unit,
    viewModel: SellerShippingViewModel = viewModel()
) {
    LaunchedEffect(productId) { viewModel.load(productId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shipping · $productName", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Weight", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = viewModel.weight.value,
                    onValueChange = { viewModel.weight.value = it },
                    label = { Text("kg") },
                    modifier = Modifier.fillMaxWidth(0.5f)
                )

                Spacer(Modifier.height(16.dp))
                Text("Parcel Size", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = viewModel.length.value,
                        onValueChange = { viewModel.length.value = it },
                        label = { Text("Length (cm)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("×")
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = viewModel.width.value,
                        onValueChange = { viewModel.width.value = it },
                        label = { Text("Width (cm)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("×")
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = viewModel.height.value,
                        onValueChange = { viewModel.height.value = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Dangerous Goods", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !viewModel.dangerousGoods.value, onClick = { viewModel.dangerousGoods.value = false })
                    Text("No")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = viewModel.dangerousGoods.value, onClick = { viewModel.dangerousGoods.value = true })
                    Text("Yes")
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                Text("Shipping Fee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Turn on a channel and set the fee for this product. Buyers will only see channels you've enabled.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))

                ShippingChannelCatalog.all.forEach { def ->
                    val enabled = viewModel.channelEnabled[def.key] ?: false
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(def.label, fontWeight = FontWeight.Medium)
                            if (enabled) {
                                OutlinedTextField(
                                    value = viewModel.channelFee[def.key] ?: "0",
                                    onValueChange = { viewModel.channelFee[def.key] = it },
                                    label = { Text("Fee ($)") },
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { viewModel.channelEnabled[def.key] = it }
                        )
                    }
                    Divider()
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                Text("Pre-Order", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !viewModel.preOrder.value, onClick = { viewModel.preOrder.value = false })
                    Text("No")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = viewModel.preOrder.value, onClick = { viewModel.preOrder.value = true })
                    Text("Yes")
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("I will ship out within")
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = viewModel.shipOutDays.value,
                        onValueChange = { viewModel.shipOutDays.value = it },
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("day(s)")
                }

                viewModel.statusMessage.value?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.save(productId, onDone = {}) },
                    enabled = !viewModel.isSaving.value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.isSaving.value) "Saving..." else "Save")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
