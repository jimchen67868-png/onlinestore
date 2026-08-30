package com.example.shopeeclone.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopeeclone.data.model.CartItem
import com.example.shopeeclone.data.model.Product
import com.example.shopeeclone.data.model.ShippingOption
import com.example.shopeeclone.data.model.ShippingOptions
import com.example.shopeeclone.data.model.Voucher
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.CartRepository
import com.example.shopeeclone.data.repository.FollowRepository
import com.example.shopeeclone.data.repository.OrderRepository
import com.example.shopeeclone.data.repository.ShippingRepository
import com.example.shopeeclone.data.repository.VoucherRepository
import kotlinx.coroutines.launch

class CartViewModel(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val voucherRepository: VoucherRepository = VoucherRepository(),
    private val followRepository: FollowRepository = FollowRepository(),
    private val shippingRepository: ShippingRepository = ShippingRepository()
) : ViewModel() {

    // SnapshotStateList (rather than mutableStateOf<List<...>>) guarantees Compose
    // observes every add/remove/update, avoiding cases where reassigning a whole
    // list doesn't reliably trigger recomposition.
    val items = mutableStateListOf<CartItem>()
    val selectedProductIds = mutableStateListOf<String>()

    val isPlacingOrder = mutableStateOf(false)
    val orderPlacedSuccessfully = mutableStateOf(false)

    val appliedVoucher = mutableStateOf<Voucher?>(null)
    val voucherErrorMessage = mutableStateOf<String?>(null)
    val isValidatingVoucher = mutableStateOf(false)

    val availableShippingOptions = mutableStateOf(ShippingOptions.all)
    val selectedShipping = mutableStateOf(ShippingOptions.all.first())
    val isLoadingShippingOptions = mutableStateOf(false)

    init {
        refresh()
    }

    fun refresh() {
        items.clear()
        items.addAll(CartRepository.items)
        val currentIds = items.map { it.product.id }
        selectedProductIds.retainAll(currentIds)
        currentIds.forEach { id -> if (id !in selectedProductIds) selectedProductIds.add(id) }
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

    fun toggleSelected(productId: String) {
        if (productId in selectedProductIds) selectedProductIds.remove(productId)
        else selectedProductIds.add(productId)
    }

    fun toggleSelectAll() {
        if (selectedProductIds.size == items.size) {
            selectedProductIds.clear()
        } else {
            selectedProductIds.clear()
            selectedProductIds.addAll(items.map { it.product.id })
        }
    }

    fun toggleSelectShop(sellerName: String) {
        val shopProductIds = items.filter { it.product.sellerName == sellerName }.map { it.product.id }
        val allSelected = shopProductIds.all { it in selectedProductIds }
        if (allSelected) {
            selectedProductIds.removeAll(shopProductIds)
        } else {
            shopProductIds.forEach { if (it !in selectedProductIds) selectedProductIds.add(it) }
        }
    }

    fun selectedItems(): List<CartItem> = items.filter { it.product.id in selectedProductIds }

    fun total(): Double = selectedItems().sumOf { it.subtotal }

    fun discountAmount(): Double =
        appliedVoucher.value?.let { voucherRepository.calculateDiscount(it, total()) } ?: 0.0

    fun shippingCost(): Double = selectedShipping.value.cost

    fun finalTotal(): Double = (total() - discountAmount() + shippingCost()).coerceAtLeast(0.0)

    /**
     * Loads shipping options based on the currently *selected* items. If every
     * selected item is from the same seller and that seller has configured their
     * own options, those are used; otherwise falls back to the default list.
     */
    fun loadShippingOptionsForCart() {
        viewModelScope.launch {
            isLoadingShippingOptions.value = true
            val sellerIds = selectedItems().map { it.product.sellerId }.distinct()
            val options = if (sellerIds.size == 1 && sellerIds.first().isNotBlank()) {
                val sellerOptions = shippingRepository.getSellerShippingOptions(sellerIds.first())
                if (sellerOptions.isNotEmpty()) {
                    sellerOptions.map { ShippingOption(it.name, it.cost, it.etaDays) }
                } else {
                    ShippingOptions.all
                }
            } else {
                ShippingOptions.all
            }
            availableShippingOptions.value = options
            selectedShipping.value = options.first()
            isLoadingShippingOptions.value = false
        }
    }

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
            val itemsToOrder = selectedItems()
            val result = orderRepository.placeOrder(
                userId = userId,
                items = itemsToOrder,
                address = address,
                discountAmount = discount,
                voucherCode = voucher?.code ?: "",
                shippingMethod = shipping.name,
                shippingCost = shipping.cost
            )
            if (result.isSuccess) {
                CartRepository.removeAll(itemsToOrder.map { it.product.id })
                if (voucher != null) voucherRepository.incrementUsage(voucher)
                appliedVoucher.value = null
            }
            isPlacingOrder.value = false
            orderPlacedSuccessfully.value = result.isSuccess
            refresh()
        }
    }
}
