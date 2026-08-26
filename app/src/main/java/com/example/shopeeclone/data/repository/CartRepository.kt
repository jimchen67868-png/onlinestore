package com.example.shopeeclone.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.shopeeclone.data.model.CartItem
import com.example.shopeeclone.data.model.Order
import com.example.shopeeclone.data.model.OrderStatus
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

// Cart persists to SharedPreferences as JSON so it survives app restarts.
// Call CartRepository.init(context) once, e.g. in MainActivity.onCreate,
// before any screen reads or writes the cart.
object CartRepository {
    private const val PREFS_NAME = "shopeeclone_cart"
    private const val KEY_ITEMS = "cart_items"

    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items

    fun init(context: Context) {
        if (prefs != null) return // already initialized
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_ITEMS, null)
        if (!saved.isNullOrBlank()) {
            try {
                _items.clear()
                _items.addAll(json.decodeFromString<List<CartItem>>(saved))
            } catch (e: Exception) {
                // Corrupt or outdated saved data — start with an empty cart rather than crash.
                _items.clear()
            }
        }
    }

    private fun persist() {
        prefs?.edit()?.putString(KEY_ITEMS, json.encodeToString(_items.toList()))?.apply()
    }

    fun add(item: CartItem) {
        val existing = _items.find { it.product.id == item.product.id }
        if (existing != null) {
            val index = _items.indexOf(existing)
            _items[index] = existing.copy(quantity = existing.quantity + item.quantity)
        } else {
            _items.add(item)
        }
        persist()
    }

    fun remove(productId: String) {
        _items.removeAll { it.product.id == productId }
        persist()
    }

    fun updateQuantity(productId: String, quantity: Int) {
        val index = _items.indexOfFirst { it.product.id == productId }
        if (index != -1) _items[index] = _items[index].copy(quantity = quantity)
        persist()
    }

    fun clear() {
        _items.clear()
        persist()
    }

    fun total(): Double = _items.sumOf { it.subtotal }
}

class OrderRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    suspend fun placeOrder(userId: String, items: List<CartItem>, address: String): Result<Order> = try {
        val order = Order(
            id = UUID.randomUUID().toString(),
            userId = userId,
            items = items,
            totalAmount = items.sumOf { it.subtotal },
            status = OrderStatus.PENDING,
            shippingAddress = address
        )
        client.postgrest["orders"].insert(order)
        CartRepository.clear()
        Result.success(order)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getOrdersForUser(userId: String): List<Order> = try {
        client.postgrest["orders"].select {
            filter { eq("user_id", userId) }
        }.decodeList<Order>()
    } catch (e: Exception) {
        emptyList()
    }
}
