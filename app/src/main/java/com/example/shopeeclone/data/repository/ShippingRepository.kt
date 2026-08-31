package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.SellerShippingChannel
import com.example.shopeeclone.data.model.SellerShippingSettings
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ShippingRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    suspend fun getSettings(sellerId: String): SellerShippingSettings? = try {
        client.postgrest["seller_shipping_settings"].select {
            filter { eq("seller_id", sellerId) }
        }.decodeSingleOrNull<SellerShippingSettings>()
    } catch (e: Exception) {
        null
    }

    suspend fun upsertSettings(settings: SellerShippingSettings): Result<Unit> = try {
        client.postgrest["seller_shipping_settings"].upsert(settings)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getChannels(sellerId: String): List<SellerShippingChannel> = try {
        client.postgrest["seller_shipping_channels"].select {
            filter { eq("seller_id", sellerId) }
        }.decodeList<SellerShippingChannel>()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun upsertChannel(channel: SellerShippingChannel): Result<Unit> = try {
        client.postgrest["seller_shipping_channels"].upsert(
            channel,
            onConflict = "seller_id,channel_key"
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
