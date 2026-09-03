package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductLike(
    @SerialName("user_id") val userId: String = "",
    @SerialName("product_id") val productId: String = ""
)
