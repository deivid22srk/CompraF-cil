package com.example.comprafacil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.CartItem
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val _cartCount = MutableStateFlow(0)
    val cartCount: StateFlow<Int> = _cartCount.asStateFlow()

    init {
        observeCart()
    }

    private fun observeCart() {
        val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
        if (userId != null) {
            fetchCount(userId)

            // Realtime is better for cross-screen updates
            viewModelScope.launch {
                try {
                    val channel = SupabaseConfig.client.realtime.channel("cart_count_$userId")
                    val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "cart_items"
                        filter = "user_id=eq.$userId"
                    }

                    changeFlow.onEach {
                        fetchCount(userId)
                    }.launchIn(this)

                    channel.subscribe()
                } catch (e: Exception) {}
            }
        }
    }

    private fun fetchCount(userId: String) {
        viewModelScope.launch {
            try {
                val items = SupabaseConfig.client.from("cart_items").select {
                    filter { eq("user_id", userId) }
                }.decodeAs<List<CartItem>>()
                _cartCount.value = items.sumOf { it.quantity }
            } catch (e: Exception) {}
        }
    }
}
