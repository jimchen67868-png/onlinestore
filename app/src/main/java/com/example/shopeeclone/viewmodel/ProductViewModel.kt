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

    private var allProducts: List<Product> = emptyList()

    val products = mutableStateOf<List<Product>>(emptyList())
    val isLoading = mutableStateOf(false)
    val searchQuery = mutableStateOf("")
    val selectedCategory = mutableStateOf("All")
    val errorMessage = mutableStateOf<String?>(null)

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            isLoading.value = true
            allProducts = repository.getProducts()
            errorMessage.value = repository.lastError
            applyFilters()
            isLoading.value = false
        }
    }

    fun search(query: String) {
        searchQuery.value = query
        applyFilters()
    }

    fun selectCategory(category: String) {
        selectedCategory.value = category
        applyFilters()
    }

    private fun applyFilters() {
        products.value = allProducts.filter { p ->
            val matchesCategory = selectedCategory.value == "All" ||
                p.category.equals(selectedCategory.value, ignoreCase = true)
            val matchesSearch = searchQuery.value.isBlank() ||
                p.name.contains(searchQuery.value, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    suspend fun getProduct(id: String): Product? {
        val result = repository.getProductById(id)
        errorMessage.value = repository.lastError
        return result
    }
}
