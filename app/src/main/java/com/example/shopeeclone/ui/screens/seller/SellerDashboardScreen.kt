package com.example.shopeeclone.ui.screens.seller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.ProductRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.util.UUID

class SellerViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {
    val statusMessage = mutableStateOf<String?>(null)
    val isSaving = mutableStateOf(false)
    val myProducts = mutableStateOf<List<Product>>(emptyList())
    val isLoadingProducts = mutableStateOf(false)

    private val sellerId: String
        get() = authRepository.currentUserId ?: "guest"

    init {
        loadMyProducts()
    }

    fun loadMyProducts() {
        viewModelScope.launch {
            isLoadingProducts.value = true
            myProducts.value = productRepository.getProductsForSeller(sellerId)
            isLoadingProducts.value = false
        }
    }

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
                    sellerId = sellerId,
                    sellerName = "My Store"
                )
                com.example.shopeeclone.data.remote.SupabaseClient.client
                    .postgrest["products"].insert(product)
                statusMessage.value = "Product listed successfully!"
                loadMyProducts()
                onDone()
            } catch (e: Exception) {
                statusMessage.value = "Failed: ${e.message}"
            } finally {
                isSaving.value = false
            }
        }
    }

    fun updateProduct(product: Product, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            val result = productRepository.updateProduct(product)
            isSaving.value = false
            result.onSuccess {
                statusMessage.value = "Product updated!"
                loadMyProducts()
                onDone()
            }.onFailure {
                statusMessage.value = "Update failed: ${it.message}"
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            val result = productRepository.deleteProduct(id)
            result.onSuccess {
                statusMessage.value = "Product deleted."
                loadMyProducts()
            }.onFailure {
                statusMessage.value = "Delete failed: ${it.message}"
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
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }

    fun clearForm() {
        name = ""; price = ""; description = ""; stock = ""
        editingProduct = null
    }

    fun startEditing(product: Product) {
        editingProduct = product
        name = product.name
        price = product.price.toString()
        description = product.description
        stock = product.stock.toString()
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Delete product?") },
            text = { Text("\"${product.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProduct(product.id)
                    productPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Dashboard") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text(
                    if (editingProduct == null) "List a New Product" else "Edit Product",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (editingProduct != null) {
                        OutlinedButton(onClick = { clearForm() }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            val current = editingProduct
                            if (current == null) {
                                viewModel.listProduct(
                                    name = name,
                                    description = description,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    stock = stock.toIntOrNull() ?: 0,
                                    onDone = { clearForm() }
                                )
                            } else {
                                val updated = current.copy(
                                    name = name,
                                    description = description,
                                    price = price.toDoubleOrNull() ?: current.price,
                                    stock = stock.toIntOrNull() ?: current.stock
                                )
                                viewModel.updateProduct(updated, onDone = { clearForm() })
                            }
                        },
                        enabled = name.isNotBlank() && price.toDoubleOrNull() != null && !viewModel.isSaving.value,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                viewModel.isSaving.value -> "Saving..."
                                editingProduct != null -> "Update Product"
                                else -> "List Product"
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                Text("My Products", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (viewModel.isLoadingProducts.value) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (viewModel.myProducts.value.isEmpty()) {
                item {
                    Text("You haven't listed any products yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(viewModel.myProducts.value, key = { it.id }) { product ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Medium)
                            Text("$${product.price} · Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { startEditing(product) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { productPendingDelete = product }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
