package com.example.comprafacil

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    val client = createSupabaseClient(
        supabaseUrl = "https://zlykhkpycrsukoaxhfzn.supabase.co",
        supabaseKey = "sb_publishable_F9BmcR4Fv39SK1Kiz3yKFQ_75DYBudY"
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
        install(io.github.jan.supabase.realtime.Realtime)
    }
}
