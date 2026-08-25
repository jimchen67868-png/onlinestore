package com.example.shopeeclone.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {

    val products = mutableStateOf<List<Product>>(emptyList())
    val isLoading = mutableStateOf(false)
    val searchQuery = mutableStateOf("")

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            isLoading.value = true
            products.value = repository.getProducts()
            isLoading.value = false
        }
    }

    fun search(query: String) {
        searchQuery.value = query
        viewModelScope.launch {
            products.value = if (query.isBlank()) repository.getProducts()
            else repository.searchProducts(query)
        }
    }

    suspend fun getProduct(id: String): Product? = repository.getProductById(id)
}
