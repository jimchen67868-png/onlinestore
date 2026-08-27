package com.example.shopeeclone.data.repository

import android.util.Log
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ProductRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    private val sampleProducts = listOf(
        Product(id = "1", name = "Wireless Earbuds", description = "Bluetooth 5.3, noise cancelling", price = 29.99, discountPrice = 19.99, category = "Electronics", sellerId = "seller1", sellerName = "TechStore", stock = 120, rating = 4.7, soldCount = 850),
        Product(id = "2", name = "Running Shoes", description = "Lightweight breathable mesh", price = 45.00, discountPrice = null, category = "Fashion", sellerId = "seller2", sellerName = "SportZone", stock = 60, rating = 4.5, soldCount = 320),
        Product(id = "3", name = "Phone Case", description = "Shockproof clear case", price = 8.99, discountPrice = 5.99, category = "Electronics", sellerId = "seller1", sellerName = "TechStore", stock = 300, rating = 4.2, soldCount = 1200),
        Product(id = "4", name = "Backpack", description = "Water-resistant 20L", price = 25.50, discountPrice = null, category = "Fashion", sellerId = "seller3", sellerName = "UrbanGear", stock = 80, rating = 4.6, soldCount = 410),
        Product(id = "5", name = "Desk Lamp", description = "LED adjustable brightness", price = 15.00, discountPrice = 12.00, category = "Home", sellerId = "seller4", sellerName = "HomeEssentials", stock = 50, rating = 4.4, soldCount = 200)
    )

    var lastError: String? = null
        private set

    suspend fun getProducts(): List<Product> = try {
        val remote = client.postgrest["products"].select().decodeList<Product>()
        lastError = null
        if (remote.isEmpty()) sampleProducts else remote
    } catch (e: Exception) {
        Log.e("ProductRepository", "Failed to fetch products from Supabase", e)
        lastError = e.message ?: e.toString()
        sampleProducts
    }

    suspend fun getProductById(id: String): Product? =
        getProducts().find { it.id == id }

    suspend fun searchProducts(query: String): List<Product> =
        getProducts().filter { it.name.contains(query, ignoreCase = true) }

    suspend fun getProductsForSeller(sellerId: String): List<Product> = try {
        val result = client.postgrest["products"].select {
            filter { eq("seller_id", sellerId) }
        }.decodeList<Product>()
        lastError = null
        result
    } catch (e: Exception) {
        Log.e("ProductRepository", "Failed to fetch seller's products", e)
        lastError = e.message ?: e.toString()
        emptyList()
    }

    suspend fun updateProduct(product: Product): Result<Unit> = try {
        client.postgrest["products"].update(product) {
            filter { eq("id", product.id) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("ProductRepository", "Failed to update product", e)
        Result.failure(e)
    }

    suspend fun deleteProduct(id: String): Result<Unit> = try {
        client.postgrest["products"].delete {
            filter { eq("id", id) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("ProductRepository", "Failed to delete product", e)
        Result.failure(e)
    }
}
