package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("buyer_name") val buyerName: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val content: String = "",
    @SerialName("media_url") val mediaUrl: String = "",
    @SerialName("media_type") val mediaType: String = "", // "image", "video", "product", "order", or "" for text-only
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("product_price") val productPrice: Double? = null,
    @SerialName("product_image_url") val productImageUrl: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("order_total") val orderTotal: Double? = null,
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("order_summary") val orderSummary: String = "",
    @SerialName("created_at") val createdAt: String? = null
)
