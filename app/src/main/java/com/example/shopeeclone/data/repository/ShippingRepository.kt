package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.SellerShippingOption
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ShippingRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    suspend fun getSellerShippingOptions(sellerId: String): List<SellerShippingOption> = try {
        client.postgrest["seller_shipping_options"].select {
            filter { eq("seller_id", sellerId) }
        }.decodeList<SellerShippingOption>()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun createOption(option: SellerShippingOption): Result<Unit> = try {
        client.postgrest["seller_shipping_options"].insert(option)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteOption(id: String): Result<Unit> = try {
        client.postgrest["seller_shipping_options"].delete {
            filter { eq("id", id) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
