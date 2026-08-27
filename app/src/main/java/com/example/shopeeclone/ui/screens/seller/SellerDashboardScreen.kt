package com.example.shopeeclone.ui.screens.seller

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.MediaRepository
import com.example.shopeeclone.data.repository.ProductRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.util.UUID

class SellerViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
    private val mediaRepository: MediaRepository = MediaRepository()
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

    fun saveProduct(
        context: Context,
        existing: Product?,
        name: String,
        description: String,
        price: Double,
        stock: Int,
        newImageUri: Uri?,
        newVideoUri: Uri?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            try {
                var imageUrl = existing?.imageUrl ?: ""
                var videoUrl = existing?.videoUrl ?: ""

                newImageUri?.let { uri ->
                    val result = mediaRepository.upload(context, uri, sellerId)
                    result.onSuccess { imageUrl = it }
                        .onFailure { throw it }
                }
                newVideoUri?.let { uri ->
                    val result = mediaRepository.upload(context, uri, sellerId)
                    result.onSuccess { videoUrl = it }
                        .onFailure { throw it }
                }

                if (existing == null) {
                    val product = Product(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        description = description,
                        price = price,
                        stock = stock,
                        sellerId = sellerId,
                        sellerName = "My Store",
                        imageUrl = imageUrl,
                        videoUrl = videoUrl
                    )
                    com.example.shopeeclone.data.remote.SupabaseClient.client
                        .postgrest["products"].insert(product)
                    statusMessage.value = "Product listed successfully!"
                } else {
                    val updated = existing.copy(
                        name = name,
                        description = description,
                        price = price,
                        stock = stock,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl
                    )
                    productRepository.updateProduct(updated).getOrThrow()
                    statusMessage.value = "Product updated!"
                }
                loadMyProducts()
                onDone()
            } catch (e: Exception) {
                statusMessage.value = "Failed: ${e.message}"
            } finally {
                isSaving.value = false
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
    onManageVouchers: () -> Unit,
    viewModel: SellerViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedImageUri = uri
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedVideoUri = uri
    }

    fun clearForm() {
        name = ""; price = ""; description = ""; stock = ""
        editingProduct = null
        pickedImageUri = null
        pickedVideoUri = null
    }

    fun startEditing(product: Product) {
        editingProduct = product
        name = product.name
        price = product.price.toString()
        description = product.description
        stock = product.stock.toString()
        pickedImageUri = null
        pickedVideoUri = null
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = onManageVouchers) { Text("Vouchers") }
                }
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

                // Photo picker + preview
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Choose Photo") }
                    Spacer(Modifier.width(12.dp))
                    val previewUrl = pickedImageUri?.toString() ?: editingProduct?.imageUrl
                    if (!previewUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = previewUrl,
                            contentDescription = "Product photo preview",
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Video picker + indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }) { Text("Choose Video") }
                    Spacer(Modifier.width(12.dp))
                    val hasVideo = pickedVideoUri != null || !editingProduct?.videoUrl.isNullOrBlank()
                    if (hasVideo) {
                        Text("Video attached ✓", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(16.dp))
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
                            viewModel.saveProduct(
                                context = context,
                                existing = editingProduct,
                                name = name,
                                description = description,
                                price = price.toDoubleOrNull() ?: 0.0,
                                stock = stock.toIntOrNull() ?: 0,
                                newImageUri = pickedImageUri,
                                newVideoUri = pickedVideoUri,
                                onDone = { clearForm() }
                            )
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
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
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
