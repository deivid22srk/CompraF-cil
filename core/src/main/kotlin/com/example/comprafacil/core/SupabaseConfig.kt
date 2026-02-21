package com.example.comprafacil.core

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SettingsSessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

object SupabaseConfig {
    private var _client: io.github.jan.supabase.SupabaseClient? = null

    val client: io.github.jan.supabase.SupabaseClient
        get() = _client ?: throw IllegalStateException("SupabaseConfig must be initialized with Context first")

    fun initialize(context: Context) {
        if (_client != null) return

        val settings = SharedPreferencesSettings(context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE))

        _client = createSupabaseClient(
            supabaseUrl = "https://zlykhkpycrsukoaxhfzn.supabase.co",
            supabaseKey = "sb_publishable_F9BmcR4Fv39SK1Kiz3yKFQ_75DYBudY"
        ) {
            install(Postgrest)
            install(Auth) {
                sessionManager = SettingsSessionManager(settings)
            }
            install(Storage)
            install(Realtime)
        }
    }
}
