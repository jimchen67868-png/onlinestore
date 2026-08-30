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
    @SerialName("shipping_method") val shippingMethod: String = "Standard",
    @SerialName("shipping_cost") val shippingCost: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null // set by DB default now()
)

data class ShippingOption(
    val name: String,
    val cost: Double,
    val etaDays: String
)

object ShippingOptions {
    val all = listOf(
        ShippingOption("Standard", 2.99, "3-5 days"),
        ShippingOption("Express", 6.99, "1-2 days"),
        ShippingOption("Free Shipping", 0.0, "5-7 days")
    )
}

@Serializable
data class SellerShippingOption(
    val id: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    val name: String = "",
    val cost: Double = 0.0,
    @SerialName("eta_days") val etaDays: String = ""
)
