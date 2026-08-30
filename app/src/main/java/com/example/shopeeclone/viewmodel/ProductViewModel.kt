package com.example.shopeeclone.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.ProductRepository
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    RELEVANCE("Default"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    BEST_SELLING("Best Selling"),
    HIGHEST_RATED("Highest Rated")
}

class ProductViewModel(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {

    private var allProducts: List<Product> = emptyList()

    val products = mutableStateOf<List<Product>>(emptyList())
    val isLoading = mutableStateOf(false)
    val searchQuery = mutableStateOf("")
    val selectedCategory = mutableStateOf("All")
    val errorMessage = mutableStateOf<String?>(null)

    val sortOption = mutableStateOf(SortOption.RELEVANCE)
    val minPrice = mutableStateOf("")
    val maxPrice = mutableStateOf("")

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

    fun setSortOption(option: SortOption) {
        sortOption.value = option
        applyFilters()
    }

    fun setPriceRange(min: String, max: String) {
        minPrice.value = min
        maxPrice.value = max
        applyFilters()
    }

    fun resetFilters() {
        sortOption.value = SortOption.RELEVANCE
        minPrice.value = ""
        maxPrice.value = ""
        applyFilters()
    }

    private fun applyFilters() {
        val minVal = minPrice.value.toDoubleOrNull()
        val maxVal = maxPrice.value.toDoubleOrNull()

        var result = allProducts.filter { p ->
            val effectivePrice = p.discountPrice ?: p.price
            val matchesCategory = selectedCategory.value == "All" ||
                p.category.equals(selectedCategory.value, ignoreCase = true)
            val matchesSearch = searchQuery.value.isBlank() ||
                p.name.contains(searchQuery.value, ignoreCase = true)
            val matchesMin = minVal == null || effectivePrice >= minVal
            val matchesMax = maxVal == null || effectivePrice <= maxVal
            matchesCategory && matchesSearch && matchesMin && matchesMax
        }

        result = when (sortOption.value) {
            SortOption.RELEVANCE -> result
            SortOption.PRICE_LOW_HIGH -> result.sortedBy { it.discountPrice ?: it.price }
            SortOption.PRICE_HIGH_LOW -> result.sortedByDescending { it.discountPrice ?: it.price }
            SortOption.BEST_SELLING -> result.sortedByDescending { it.soldCount }
            SortOption.HIGHEST_RATED -> result.sortedByDescending { it.rating }
        }

        products.value = result
    }

    suspend fun getProduct(id: String): Product? {
        val result = repository.getProductById(id)
        errorMessage.value = repository.lastError
        return result
    }
}
