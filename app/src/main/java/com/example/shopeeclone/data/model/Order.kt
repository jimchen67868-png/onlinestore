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
data class SellerShippingSettings(
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    @SerialName("length_cm") val lengthCm: Double = 0.0,
    @SerialName("width_cm") val widthCm: Double = 0.0,
    @SerialName("height_cm") val heightCm: Double = 0.0,
    @SerialName("dangerous_goods") val dangerousGoods: Boolean = false,
    @SerialName("pre_order") val preOrder: Boolean = false,
    @SerialName("ship_out_days") val shipOutDays: Int = 1
)

@Serializable
data class SellerShippingChannel(
    val id: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("channel_key") val channelKey: String = "",
    val enabled: Boolean = false,
    val fee: Double = 0.0
)
