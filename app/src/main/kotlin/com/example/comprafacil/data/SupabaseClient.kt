package com.example.comprafacil.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    const val URL = "https://zlykhkpycrsukoaxhfzn.supabase.co"
    const val KEY = "sb_publishable_F9BmcR4Fv39SK1Kiz3yKFQ_75DYBudY"

    val client = createSupabaseClient(URL, KEY) {
        install(Postgrest)
        install(Storage)
    }
}
