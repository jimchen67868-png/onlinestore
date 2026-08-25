package com.example.shopeeclone.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopeeclone.data.model.CartItem
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.CartRepository
import com.example.shopeeclone.data.repository.OrderRepository
import kotlinx.coroutines.launch

class CartViewModel(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    val items = mutableStateOf<List<CartItem>>(CartRepository.items)
    val isPlacingOrder = mutableStateOf(false)
    val orderPlacedSuccessfully = mutableStateOf(false)

    fun refresh() {
        items.value = CartRepository.items.toList()
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        CartRepository.add(CartItem(product, quantity))
        refresh()
    }

    fun removeFromCart(productId: String) {
        CartRepository.remove(productId)
        refresh()
    }

    fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) removeFromCart(productId)
        else {
            CartRepository.updateQuantity(productId, quantity)
            refresh()
        }
    }

    fun total(): Double = CartRepository.total()

    fun placeOrder(address: String) {
        viewModelScope.launch {
            isPlacingOrder.value = true
            val userId = authRepository.currentUserId ?: "guest"
            val result = orderRepository.placeOrder(userId, CartRepository.items, address)
            isPlacingOrder.value = false
            orderPlacedSuccessfully.value = result.isSuccess
            refresh()
        }
    }
}
