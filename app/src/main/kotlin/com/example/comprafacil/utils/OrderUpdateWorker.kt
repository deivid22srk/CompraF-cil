package com.example.comprafacil.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Order
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from

class OrderUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        SupabaseConfig.initialize(applicationContext)
        val client = SupabaseConfig.client
        val userId = client.auth.currentUserOrNull()?.id ?: return Result.success()
        val notificationHelper = NotificationHelper(applicationContext)
        val settings = SharedPreferencesSettings(applicationContext.getSharedPreferences("order_notifications", Context.MODE_PRIVATE))

        try {
            // Fetch active orders (non-concluded)
            val orders = client.from("orders").select {
                filter {
                    eq("user_id", userId)
                    neq("status", "concluído")
                    neq("status", "cancelado")
                }
            }.decodeAs<List<Order>>()

            for (order in orders) {
                val orderId = order.id ?: continue
                val status = order.status ?: "pendente"
                val lastStatus = settings.getString("order_$orderId", "pendente")

                if (status != lastStatus) {
                    notificationHelper.showNotification(
                        "Atualização no Pedido #${orderId.takeLast(6)}",
                        "O status mudou para: ${status.uppercase()}"
                    )
                    settings.putString("order_$orderId", status)
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
