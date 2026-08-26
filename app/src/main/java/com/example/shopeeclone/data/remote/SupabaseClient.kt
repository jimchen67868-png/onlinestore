package com.example.shopeeclone.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

object SupabaseClient {
    private const val SUPABASE_URL = "https://jzmupzndkinwghneiazc.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp6bXVwem5ka2lud2dobmVpYXpjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc2NDU5ODYsImV4cCI6MjEwMzIyMTk4Nn0.GaEtNVwsPLanPTYe8IHsgK7LGSCsfSNQWnuec2WzuVg"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        requestTimeout = 15.seconds
    }
}
