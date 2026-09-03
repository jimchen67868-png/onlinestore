package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.Review
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID

class ReviewRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client,
    private val productRepository: ProductRepository = ProductRepository()
) {
    suspend fun getReviewsForProduct(productId: String): List<Review> = try {
        client.postgrest["product_reviews"].select {
            filter { eq("product_id", productId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Review>()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun getMyReview(productId: String, buyerId: String): Review? = try {
        client.postgrest["product_reviews"].select {
            filter {
                eq("product_id", productId)
                eq("buyer_id", buyerId)
            }
        }.decodeSingleOrNull<Review>()
    } catch (e: Exception) {
        null
    }

    /** Inserts a new review, or updates the buyer's existing one for this product (one review per buyer per product). */
    suspend fun submitReview(
        productId: String,
        buyerId: String,
        buyerName: String,
        rating: Int,
        comment: String
    ): Result<Unit> = try {
        val existing = getMyReview(productId, buyerId)
        val review = Review(
            id = existing?.id ?: UUID.randomUUID().toString(),
            productId = productId,
            buyerId = buyerId,
            buyerName = buyerName,
            rating = rating,
            comment = comment
        )
        if (existing != null) {
            client.postgrest["product_reviews"].update(review) {
                filter { eq("id", existing.id) }
            }
        } else {
            client.postgrest["product_reviews"].insert(review)
        }
        recalculateProductRating(productId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun recalculateProductRating(productId: String) {
        val reviews = getReviewsForProduct(productId)
        if (reviews.isEmpty()) return
        val average = reviews.map { it.rating }.average()
        val product = productRepository.getProductById(productId) ?: return
        productRepository.updateProduct(product.copy(rating = average))
    }
}
