package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.CartItem
import com.example.shopeeclone.data.model.Order
import com.example.shopeeclone.data.model.OrderStatus
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.util.UUID

// Cart lives in memory (per-session); swap for Room/local DB if you want it to persist.
object CartRepository {
    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items

    fun add(item: CartItem) {
        val existing = _items.find { it.product.id == item.product.id }
        if (existing != null) {
            val index = _items.indexOf(existing)
            _items[index] = existing.copy(quantity = existing.quantity + item.quantity)
        } else {
            _items.add(item)
        }
    }

    fun remove(productId: String) {
        _items.removeAll { it.product.id == productId }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        val index = _items.indexOfFirst { it.product.id == productId }
        if (index != -1) _items[index] = _items[index].copy(quantity = quantity)
    }

    fun clear() = _items.clear()

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
