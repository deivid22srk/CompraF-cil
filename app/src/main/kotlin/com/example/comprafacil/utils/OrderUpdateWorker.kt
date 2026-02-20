package com.example.comprafacil.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.comprafacil.SupabaseConfig
import com.example.comprafacil.data.Order
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from

class OrderUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val client = SupabaseConfig.client
        val userId = client.auth.currentUserOrNull()?.id ?: return Result.success()
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            // In a real app, we'd compare with a local database of last known statuses
            // For this demo, we'll just fetch the latest orders
            val latestOrders = client.from("orders").select {
                filter { eq("user_id", userId) }
                // Limit to recent orders
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeAs<List<Order>>()

            if (latestOrders.isNotEmpty()) {
                val latest = latestOrders[0]
                // Only notify if status is not 'pendente' (meaning it has been processed/changed)
                if (latest.status != "pendente" && latest.status != "concluído") {
                    notificationHelper.showNotification(
                        "Status do Pedido",
                        "O pedido #${latest.id?.takeLast(6)} está ${latest.status}"
                    )
                }
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
