package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.Voucher
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoucherRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun validateVoucher(code: String, subtotal: Double): Result<Voucher> {
        return try {
            val voucher = client.postgrest["vouchers"].select {
                filter { eq("code", code.trim().uppercase()) }
            }.decodeSingleOrNull<Voucher>()
                ?: return Result.failure(Exception("Voucher code not found"))

            voucher.expiresAt?.let { expiryStr ->
                try {
                    val expiry = dateFormat.parse(expiryStr)
                    if (expiry != null && expiry.before(Date())) {
                        return Result.failure(Exception("This voucher has expired"))
                    }
                } catch (e: Exception) {
                    // Unparseable date — ignore expiry check rather than block a valid voucher.
                }
            }

            if (voucher.usageLimit > 0 && voucher.timesUsed >= voucher.usageLimit) {
                return Result.failure(Exception("This voucher has reached its usage limit"))
            }

            if (subtotal < voucher.minSpend) {
                return Result.failure(Exception("Minimum spend of $${"%.2f".format(voucher.minSpend)} required"))
            }

            Result.success(voucher)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateDiscount(voucher: Voucher, subtotal: Double): Double {
        val raw = if (voucher.discountType == "percentage")
            subtotal * (voucher.discountValue / 100.0)
        else
            voucher.discountValue
        val capped = voucher.maxDiscount?.let { minOf(raw, it) } ?: raw
        return minOf(capped, subtotal)
    }

    suspend fun incrementUsage(voucher: Voucher): Result<Unit> = try {
        client.postgrest["vouchers"].update(
            VoucherUsageUpdate(timesUsed = voucher.timesUsed + 1)
        ) {
            filter { eq("id", voucher.id) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getSellerVouchers(sellerId: String): List<Voucher> = try {
        client.postgrest["vouchers"].select {
            filter { eq("seller_id", sellerId) }
        }.decodeList<Voucher>()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun createVoucher(voucher: Voucher): Result<Unit> = try {
        client.postgrest["vouchers"].insert(voucher)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteVoucher(id: String): Result<Unit> = try {
        client.postgrest["vouchers"].delete {
            filter { eq("id", id) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@kotlinx.serialization.Serializable
private data class VoucherUsageUpdate(
    @kotlinx.serialization.SerialName("times_used") val timesUsed: Int
)
