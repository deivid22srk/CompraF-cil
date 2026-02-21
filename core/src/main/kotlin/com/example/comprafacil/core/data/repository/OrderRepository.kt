package com.example.comprafacil.core.data.repository

import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Order
import com.example.comprafacil.core.data.OrderItem
import com.example.comprafacil.core.data.OrderStatusHistory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SupabaseOrder

class OrderRepository {
    private val client = SupabaseConfig.client

    suspend fun getOrders(userId: String? = null): List<Order> {
        return client.from("orders").select {
            if (userId != null) {
                filter { eq("user_id", userId) }
            }
            order("created_at", SupabaseOrder.DESCENDING)
        }.decodeAs<List<Order>>()
    }

    suspend fun getOrderItems(orderId: String): List<OrderItem> {
        return client.from("order_items").select {
            filter { eq("order_id", orderId) }
        }.decodeAs<List<OrderItem>>()
    }

    suspend fun updateOrderStatus(orderId: String, status: String, notes: String? = null) {
        client.from("orders").update({
            set("status", status)
        }) {
            filter { eq("id", orderId) }
        }

        val history = OrderStatusHistory(
            order_id = orderId,
            status = status,
            notes = notes
        )
        client.from("order_status_history").insert(history)
    }

    suspend fun deleteOrder(orderId: String) {
        client.from("orders").delete {
            filter { eq("id", orderId) }
        }
    }
}
