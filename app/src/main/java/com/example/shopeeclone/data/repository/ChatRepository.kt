package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.Message
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID

class ChatRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client,
    private val authRepository: AuthRepository = AuthRepository()
) {
    suspend fun sendMessage(
        buyerId: String,
        buyerName: String,
        sellerId: String,
        sellerName: String,
        content: String
    ): Result<Unit> = try {
        val senderId = authRepository.currentUserId ?: throw IllegalStateException("Not logged in")
        val message = Message(
            id = UUID.randomUUID().toString(),
            buyerId = buyerId,
            buyerName = buyerName,
            sellerId = sellerId,
            sellerName = sellerName,
            senderId = senderId,
            content = content
        )
        client.postgrest["messages"].insert(message)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getMessages(buyerId: String, sellerId: String): List<Message> = try {
        client.postgrest["messages"].select {
            filter {
                eq("buyer_id", buyerId)
                eq("seller_id", sellerId)
            }
            order("created_at", Order.ASCENDING)
        }.decodeList<Message>()
    } catch (e: Exception) {
        emptyList()
    }

    /** Returns the most recent message for each conversation the current user is part of. */
    suspend fun getConversations(): List<Message> {
        val uid = authRepository.currentUserId ?: return emptyList()
        return try {
            val asBuyer = client.postgrest["messages"].select {
                filter { eq("buyer_id", uid) }
                order("created_at", Order.DESCENDING)
            }.decodeList<Message>()
            val asSeller = client.postgrest["messages"].select {
                filter { eq("seller_id", uid) }
                order("created_at", Order.DESCENDING)
            }.decodeList<Message>()

            (asBuyer + asSeller)
                .groupBy { it.buyerId to it.sellerId }
                .values
                .mapNotNull { group -> group.maxByOrNull { it.createdAt ?: "" } }
                .sortedByDescending { it.createdAt ?: "" }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
