package com.example.shopeeclone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Voucher(
    val id: String = "",
    val code: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("discount_type") val discountType: String = "fixed", // "fixed" or "percentage"
    @SerialName("discount_value") val discountValue: Double = 0.0,
    @SerialName("min_spend") val minSpend: Double = 0.0,
    @SerialName("max_discount") val maxDiscount: Double? = null,
    @SerialName("usage_limit") val usageLimit: Int = 0, // 0 = unlimited
    @SerialName("times_used") val timesUsed: Int = 0,
    @SerialName("expires_at") val expiresAt: String? = null // "yyyy-MM-dd", null = never expires
)
