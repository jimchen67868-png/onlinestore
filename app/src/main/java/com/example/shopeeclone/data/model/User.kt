package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    @SerialName("is_seller") val isSeller: Boolean = false,
    @SerialName("profile_image_url") val profileImageUrl: String = ""
)
