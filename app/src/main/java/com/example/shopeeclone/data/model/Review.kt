package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("buyer_name") val buyerName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    @SerialName("created_at") val createdAt: String? = null
)
