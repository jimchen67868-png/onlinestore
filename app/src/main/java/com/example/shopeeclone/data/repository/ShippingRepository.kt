package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.ProductShippingChannel
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class ShippingRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    suspend fun getProductChannels(productId: String): List<ProductShippingChannel> = try {
        client.postgrest["product_shipping_channels"].select {
            filter { eq("product_id", productId) }
        }.decodeList<ProductShippingChannel>()
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Inserts or updates a product's shipping channel row. Implemented as an
     * explicit select-then-insert-or-update (rather than relying on Postgrest's
     * upsert helper) since that avoids depending on the exact upsert/onConflict
     * signature of the Supabase-kt version in use.
     */
    suspend fun upsertProductChannel(channel: ProductShippingChannel): Result<Unit> = try {
        val existing = client.postgrest["product_shipping_channels"].select {
            filter {
                eq("product_id", channel.productId)
                eq("channel_key", channel.channelKey)
            }
        }.decodeSingleOrNull<ProductShippingChannel>()

        if (existing != null) {
            client.postgrest["product_shipping_channels"].update(
                channel.copy(id = existing.id)
            ) {
                filter { eq("id", existing.id) }
            }
        } else {
            client.postgrest["product_shipping_channels"].insert(channel)
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
