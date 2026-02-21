package com.example.comprafacil.admin.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Order
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from

class NewOrderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        SupabaseConfig.initialize(applicationContext)
        val client = SupabaseConfig.client
        val notificationHelper = NotificationHelper(applicationContext)
        val settings = SharedPreferencesSettings(applicationContext.getSharedPreferences("admin_order_notifs", Context.MODE_PRIVATE))

        try {
            // Fetch the most recent orders
            val orders = client.from("orders").select {
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(5)
            }.decodeAs<List<Order>>()

            for (order in orders) {
                val orderId = order.id ?: continue
                val isNotified = settings.getBoolean("notified_$orderId", false)

                if (!isNotified) {
                    notificationHelper.showNotification(
                        "Novo Pedido Recebido!",
                        "Pedido #${orderId.takeLast(6)} - R$ ${String.format("%.2f", order.total_price)}"
                    )
                    settings.putBoolean("notified_$orderId", true)
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
