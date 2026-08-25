package com.example.shopeeclone.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Central Supabase client. Fill in your project URL and anon key below —
 * find them in your Supabase project: Settings -> API.
 * The anon key is safe to ship in the app; row-level security policies
 * control what it can actually read/write.
 */
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
    }
}
