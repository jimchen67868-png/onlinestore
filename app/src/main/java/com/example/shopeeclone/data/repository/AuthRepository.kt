package com.example.shopeeclone.data.repository

import com.example.shopeeclone.data.model.User
import com.example.shopeeclone.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    val currentUserId: String?
        get() = client.auth.currentUserOrNull()?.id

    suspend fun signUp(name: String, email: String, password: String): Result<User> = try {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val uid = client.auth.currentUserOrNull()?.id ?: throw Exception("Sign up failed")
        val user = User(uid = uid, name = name, email = email)
        client.postgrest["users"].insert(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun login(email: String, password: String): Result<String> = try {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        Result.success(client.auth.currentUserOrNull()?.id ?: "")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun logout() {
        client.auth.signOut()
    }

    suspend fun getUserProfile(): User? = try {
        val uid = currentUserId ?: return null
        client.postgrest["users"].select {
            filter { eq("uid", uid) }
        }.decodeSingleOrNull<User>()
    } catch (e: Exception) {
        null
    }

    suspend fun updateUserProfile(name: String, phone: String, address: String): Result<Unit> = try {
        val uid = currentUserId ?: throw IllegalStateException("Not logged in")
        client.postgrest["users"].update(
            ProfileUpdate(name = name, phone = phone, address = address)
        ) {
            filter { eq("uid", uid) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@kotlinx.serialization.Serializable
private data class ProfileUpdate(
    val name: String,
    val phone: String,
    val address: String
)
