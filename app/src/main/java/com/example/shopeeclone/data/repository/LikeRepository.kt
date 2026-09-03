package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.ProductLike
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class LikeRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client,
    private val authRepository: AuthRepository = AuthRepository()
) {
    suspend fun isLiked(productId: String): Boolean {
        val uid = authRepository.currentUserId ?: return false
        return try {
            client.postgrest["product_likes"].select {
                filter {
                    eq("user_id", uid)
                    eq("product_id", productId)
                }
            }.decodeList<ProductLike>().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getLikeCount(productId: String): Int = try {
        client.postgrest["product_likes"].select {
            filter { eq("product_id", productId) }
        }.decodeList<ProductLike>().size
    } catch (e: Exception) {
        0
    }

    suspend fun like(productId: String): Result<Unit> = try {
        val uid = authRepository.currentUserId ?: throw IllegalStateException("Not logged in")
        client.postgrest["product_likes"].insert(ProductLike(userId = uid, productId = productId))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unlike(productId: String): Result<Unit> = try {
        val uid = authRepository.currentUserId ?: throw IllegalStateException("Not logged in")
        client.postgrest["product_likes"].delete {
            filter {
                eq("user_id", uid)
                eq("product_id", productId)
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getMyLikedProductIds(): List<String> {
        val uid = authRepository.currentUserId ?: return emptyList()
        return try {
            client.postgrest["product_likes"].select {
                filter { eq("user_id", uid) }
            }.decodeList<ProductLike>().map { it.productId }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
