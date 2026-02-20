package com.example.comprafacil

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SettingsSessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

object SupabaseConfig {
    val client = createSupabaseClient(
        supabaseUrl = "https://zlykhkpycrsukoaxhfzn.supabase.co",
        supabaseKey = "sb_publishable_F9BmcR4Fv39SK1Kiz3yKFQ_75DYBudY"
    ) {
        install(Postgrest)
        install(Auth) {
            sessionManager = SettingsSessionManager()
        }
        install(Storage)
        install(Realtime)
    }
}
