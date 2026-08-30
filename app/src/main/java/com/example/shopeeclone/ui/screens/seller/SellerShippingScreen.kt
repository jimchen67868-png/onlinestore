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
import com.example.shopeeclone.data.model.SellerShippingOption
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.ShippingRepository
import kotlinx.coroutines.launch
import java.util.UUID

class SellerShippingViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val shippingRepository: ShippingRepository = ShippingRepository()
) : ViewModel() {
    val options = mutableStateOf<List<SellerShippingOption>>(emptyList())
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
            options.value = shippingRepository.getSellerShippingOptions(sellerId)
            isLoading.value = false
        }
    }

    fun createOption(name: String, cost: Double, etaDays: String, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            val option = SellerShippingOption(
                id = UUID.randomUUID().toString(),
                sellerId = sellerId,
                name = name,
                cost = cost,
                etaDays = etaDays
            )
            val result = shippingRepository.createOption(option)
            isSaving.value = false
            result.onSuccess {
                statusMessage.value = "Shipping option added!"
                load()
                onDone()
            }.onFailure {
                statusMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun deleteOption(id: String) {
        viewModelScope.launch {
            shippingRepository.deleteOption(id).onSuccess { load() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerShippingScreen(
    onBack: () -> Unit,
    viewModel: SellerShippingViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var etaDays by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shipping Options") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text(
                    "Buyers will see these shipping options at checkout when their order contains only your products. If you don't add any, they'll see the default Standard / Express / Free options.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Text("Add Shipping Option", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Method name (e.g. Standard, Express, Same-Day)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Cost ($, 0 for free)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = etaDays,
                    onValueChange = { etaDays = it },
                    label = { Text("Estimated delivery (e.g. 3-5 days)") },
                    modifier = Modifier.fillMaxWidth()
                )

                viewModel.statusMessage.value?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.createOption(
                            name = name,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            etaDays = etaDays,
                            onDone = { name = ""; cost = ""; etaDays = "" }
                        )
                    },
                    enabled = name.isNotBlank() && cost.toDoubleOrNull() != null && !viewModel.isSaving.value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.isSaving.value) "Adding..." else "Add Option")
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                Text("My Shipping Options", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (viewModel.isLoading.value) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (viewModel.options.value.isEmpty()) {
                item { Text("No custom shipping options yet — buyers will see the default list.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(viewModel.options.value, key = { it.id }) { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${if (option.cost > 0) "$${"%.2f".format(option.cost)}" else "Free"} · ${option.etaDays}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { viewModel.deleteOption(option.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
