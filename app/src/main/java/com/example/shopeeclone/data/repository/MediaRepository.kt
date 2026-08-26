package com.example.shopeeclone.data.repository

import android.content.Context
import android.net.Uri
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.util.UUID

/**
 * Uploads a picked image or video to the "product-media" Supabase Storage bucket
 * and returns its public URL. Make sure that bucket exists and is public
 * (see README) before calling this.
 */
class MediaRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    private val bucket = client.storage["product-media"]

    /** Returns "image" or "video" based on the content resolver's MIME type, or null if neither. */
    fun mediaKind(context: Context, uri: Uri): String? {
        val type = context.contentResolver.getType(uri) ?: return null
        return when {
            type.startsWith("image/") -> "image"
            type.startsWith("video/") -> "video"
            else -> null
        }
    }

    suspend fun upload(context: Context, uri: Uri, sellerId: String): Result<String> = try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not read the selected file")

        val extension = when (mediaKind(context, uri)) {
            "video" -> "mp4"
            else -> "jpg"
        }
        val path = "$sellerId/${UUID.randomUUID()}.$extension"

        bucket.upload(path, bytes)
        val publicUrl = bucket.publicUrl(path)
        Result.success(publicUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
