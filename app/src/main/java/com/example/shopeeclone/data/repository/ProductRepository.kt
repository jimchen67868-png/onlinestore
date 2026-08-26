package com.example.shopeeclone.data.repository

import android.util.Log
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ProductRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    // Sample data so the UI is browsable before Supabase is configured/populated.
    private val sampleProducts = listOf(
        Product("1", "Wireless Earbuds", "Bluetooth 5.3, noise cancelling", 29.99, 19.99, "", "Electronics", "seller1", "TechStore", 120, 4.7, 850),
        Product("2", "Running Shoes", "Lightweight breathable mesh", 45.00, null, "", "Fashion", "seller2", "SportZone", 60, 4.5, 320),
        Product("3", "Phone Case", "Shockproof clear case", 8.99, 5.99, "", "Electronics", "seller1", "TechStore", 300, 4.2, 1200),
        Product("4", "Backpack", "Water-resistant 20L", 25.50, null, "", "Fashion", "seller3", "UrbanGear", 80, 4.6, 410),
        Product("5", "Desk Lamp", "LED adjustable brightness", 15.00, 12.00, "", "Home", "seller4", "HomeEssentials", 50, 4.4, 200)
    )

    // Exposes the last fetch failure (if any) so the UI can show a real message
    // instead of silently falling back and leaving the user guessing.
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
