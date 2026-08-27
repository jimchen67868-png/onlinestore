package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.Follow
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class FollowRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client,
    private val authRepository: AuthRepository = AuthRepository()
) {
    suspend fun isFollowing(sellerId: String): Boolean {
        val uid = authRepository.currentUserId ?: return false
        return try {
            client.postgrest["follows"].select {
                filter {
                    eq("follower_id", uid)
                    eq("seller_id", sellerId)
                }
            }.decodeList<Follow>().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun follow(sellerId: String, sellerName: String): Result<Unit> = try {
        val uid = authRepository.currentUserId ?: throw IllegalStateException("Not logged in")
        client.postgrest["follows"].insert(
            Follow(followerId = uid, sellerId = sellerId, sellerName = sellerName)
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unfollow(sellerId: String): Result<Unit> = try {
        val uid = authRepository.currentUserId ?: throw IllegalStateException("Not logged in")
        client.postgrest["follows"].delete {
            filter {
                eq("follower_id", uid)
                eq("seller_id", sellerId)
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getFollowedShops(): List<Follow> {
        val uid = authRepository.currentUserId ?: return emptyList()
        return try {
            client.postgrest["follows"].select {
                filter { eq("follower_id", uid) }
            }.decodeList<Follow>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
