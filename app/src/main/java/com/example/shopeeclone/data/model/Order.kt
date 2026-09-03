package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus(val label: String) {
    UNPAID("To Pay"),
    TO_SHIP("To Ship"),
    SHIPPED("To Receive"),
    COMPLETED("Completed"),
    RETURN_REQUESTED("Return/Refund"),
    REFUNDED("Refunded"),
    CANCELLED("Cancelled")
}

@Serializable
data class Order(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val items: List<CartItem> = emptyList(),
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("voucher_code") val voucherCode: String = "",
    val status: OrderStatus = OrderStatus.UNPAID,
    @SerialName("shipping_address") val shippingAddress: String = "",
    @SerialName("shipping_method") val shippingMethod: String = "Standard",
    @SerialName("shipping_cost") val shippingCost: Double = 0.0,
    @SerialName("return_reason") val returnReason: String = "",
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

data class ShippingChannelDef(val key: String, val label: String)

object ShippingChannelCatalog {
    val all = listOf(
        ShippingChannelDef("standard", "Standard Doorstep Delivery"),
        ShippingChannelDef("express", "Express Doorstep Delivery"),
        ShippingChannelDef("self_collection", "Self Collection"),
        ShippingChannelDef("bulky", "Bulky Delivery"),
        ShippingChannelDef("instant", "Instant Delivery (60 mins)")
    )

    fun labelFor(key: String): String = all.find { it.key == key }?.label ?: key
}

@Serializable
data class ProductShippingChannel(
    val id: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("channel_key") val channelKey: String = "",
    val enabled: Boolean = false,
    val fee: Double = 0.0
)
