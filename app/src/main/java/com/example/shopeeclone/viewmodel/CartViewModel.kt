package com.example.shopeeclone.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopeeclone.data.model.CartItem
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.model.ShippingOptions
import com.example.shopeeclone.data.model.Voucher
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.CartRepository
import com.example.shopeeclone.data.repository.FollowRepository
import com.example.shopeeclone.data.repository.OrderRepository
import com.example.shopeeclone.data.repository.VoucherRepository
import kotlinx.coroutines.launch

class CartViewModel(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val voucherRepository: VoucherRepository = VoucherRepository(),
    private val followRepository: FollowRepository = FollowRepository()
) : ViewModel() {

    // SnapshotStateList (rather than mutableStateOf<List<...>>) guarantees Compose
    // observes every add/remove/update, avoiding cases where reassigning a whole
    // list doesn't reliably trigger recomposition.
    val items = mutableStateListOf<CartItem>().apply { addAll(CartRepository.items) }
    val isPlacingOrder = mutableStateOf(false)
    val orderPlacedSuccessfully = mutableStateOf(false)

    val appliedVoucher = mutableStateOf<Voucher?>(null)
    val voucherErrorMessage = mutableStateOf<String?>(null)
    val isValidatingVoucher = mutableStateOf(false)

    val selectedShipping = mutableStateOf(ShippingOptions.all.first())

    fun refresh() {
        items.clear()
        items.addAll(CartRepository.items)
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

    fun discountAmount(): Double =
        appliedVoucher.value?.let { voucherRepository.calculateDiscount(it, total()) } ?: 0.0

    fun shippingCost(): Double = selectedShipping.value.cost

    fun finalTotal(): Double = (total() - discountAmount() + shippingCost()).coerceAtLeast(0.0)

    fun applyVoucher(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            isValidatingVoucher.value = true
            voucherErrorMessage.value = null
            val result = voucherRepository.validateVoucher(code, total())
            result.onSuccess { voucher ->
                if (voucher.voucherType == "follow" && !followRepository.isFollowing(voucher.sellerId)) {
                    appliedVoucher.value = null
                    voucherErrorMessage.value = "Follow this shop to use this voucher"
                } else {
                    appliedVoucher.value = voucher
                }
            }.onFailure {
                appliedVoucher.value = null
                voucherErrorMessage.value = it.message ?: "Invalid voucher"
            }
            isValidatingVoucher.value = false
        }
    }

    fun removeVoucher() {
        appliedVoucher.value = null
        voucherErrorMessage.value = null
    }

    fun placeOrder(address: String) {
        viewModelScope.launch {
            isPlacingOrder.value = true
            val userId = authRepository.currentUserId ?: "guest"
            val voucher = appliedVoucher.value
            val discount = discountAmount()
            val shipping = selectedShipping.value
            val result = orderRepository.placeOrder(
                userId = userId,
                items = CartRepository.items,
                address = address,
                discountAmount = discount,
                voucherCode = voucher?.code ?: "",
                shippingMethod = shipping.name,
                shippingCost = shipping.cost
            )
            if (result.isSuccess && voucher != null) {
                voucherRepository.incrementUsage(voucher)
            }
            isPlacingOrder.value = false
            orderPlacedSuccessfully.value = result.isSuccess
            if (result.isSuccess) {
                appliedVoucher.value = null
            }
            refresh()
        }
    }
}
