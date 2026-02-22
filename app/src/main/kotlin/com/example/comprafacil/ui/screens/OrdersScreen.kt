package com.example.comprafacil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Order
import com.example.comprafacil.core.data.OrderStatusHistory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onBack: () -> Unit) {
    val client = SupabaseConfig.client
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(Unit) {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                orders = client.from("orders").select {
                    filter { eq("user_id", userId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeAs<List<Order>>()
            } catch (e: Exception) { /* handle error */ }
        }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Meus Pedidos") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Você ainda não fez nenhum pedido.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                items(orders) { order ->
                    OrderCard(order) { selectedOrder = order }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (selectedOrder != null) {
        OrderDetailsDialog(order = selectedOrder!!, onDismiss = { selectedOrder = null })
    }
}

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pedido #${order.id?.takeLast(6)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                StatusChip(status = order.status ?: "pendente")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total: R$ ${String.format("%.2f", order.total_price)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Entrega em: ${order.location}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text("Data: ${order.created_at?.split("T")?.get(0) ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when(status) {
        "pendente" -> Color.Gray
        "aceito" -> Color(0xFF2196F3)
        "saindo para entrega" -> Color(0xFFFF9800)
        "concluído" -> Color(0xFF4CAF50)
        else -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            status.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun OrderDetailsDialog(order: Order, onDismiss: () -> Unit) {
    var history by remember { mutableStateOf<List<OrderStatusHistory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(order.id) {
        try {
            history = SupabaseConfig.client.from("order_status_history").select {
                filter { eq("order_id", order.id!!) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeAs<List<OrderStatusHistory>>()
        } catch (e: Exception) {} finally {
            loading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).padding(24.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Detalhes do Pedido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Pedido #${order.id?.takeLast(8)}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text("Total: R$ ${String.format("%.2f", order.total_price)}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pagamento: ${order.payment_method.uppercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Histórico de Status", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else if (history.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Gray, CircleShape))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Pendente", fontWeight = FontWeight.Bold)
                                Text("Pedido realizado", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(history) { item ->
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                        Box(modifier = Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(item.status.uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(item.notes ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(item.created_at?.split("T")?.get(0) ?: "", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("FECHAR")
                    }
                }
            }
        }
    }
}
