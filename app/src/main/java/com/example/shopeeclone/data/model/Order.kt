package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus { PENDING, PAID, SHIPPED, DELIVERED, CANCELLED }

@Serializable
data class Order(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val items: List<CartItem> = emptyList(),
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("voucher_code") val voucherCode: String = "",
    val status: OrderStatus = OrderStatus.PENDING,
    @SerialName("shipping_address") val shippingAddress: String = "",
    @SerialName("created_at") val createdAt: String? = null // set by DB default now()
)
