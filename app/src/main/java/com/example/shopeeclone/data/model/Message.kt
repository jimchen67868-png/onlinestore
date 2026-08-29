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
    @SerialName("media_type") val mediaType: String = "", // "image", "video", or "" for text-only
    @SerialName("created_at") val createdAt: String? = null
)
