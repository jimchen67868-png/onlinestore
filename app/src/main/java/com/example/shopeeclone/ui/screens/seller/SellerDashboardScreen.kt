package com.example.shopeeclone.ui.screens.seller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.remote.SupabaseClient
import com.example.shopeeclone.data.repository.AuthRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.util.UUID

class SellerViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val statusMessage = mutableStateOf<String?>(null)
    val isSaving = mutableStateOf(false)

    fun listProduct(name: String, description: String, price: Double, stock: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            try {
                val product = Product(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    price = price,
                    stock = stock,
                    sellerId = authRepository.currentUserId ?: "guest",
                    sellerName = "My Store"
                )
                SupabaseClient.client.postgrest["products"].insert(product)
                statusMessage.value = "Product listed successfully!"
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
fun SellerDashboardScreen(
    onBack: () -> Unit,
    viewModel: SellerViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Dashboard") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("List a New Product", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock quantity") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

            viewModel.statusMessage.value?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.listProduct(
                        name = name,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0,
                        onDone = { name = ""; price = ""; description = ""; stock = "" }
                    )
                },
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null && !viewModel.isSaving.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isSaving.value) "Listing..." else "List Product")
            }
        }
    }
}
