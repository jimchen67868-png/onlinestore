package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Follow(
    @SerialName("follower_id") val followerId: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("seller_name") val sellerName: String = ""
)
