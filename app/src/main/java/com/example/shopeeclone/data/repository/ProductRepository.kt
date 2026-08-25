package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

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

    suspend fun getProducts(): List<Product> = try {
        val remote = client.postgrest["products"].select().decodeList<Product>()
        if (remote.isEmpty()) sampleProducts else remote
    } catch (e: Exception) {
        sampleProducts
    }

    suspend fun getProductById(id: String): Product? =
        getProducts().find { it.id == id }

    suspend fun searchProducts(query: String): List<Product> =
        getProducts().filter { it.name.contains(query, ignoreCase = true) }
}
