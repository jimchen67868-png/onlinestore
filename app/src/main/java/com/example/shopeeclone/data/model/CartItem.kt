package com.example.shopeeclone.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = (product.discountPrice ?: product.price) * quantity
}
