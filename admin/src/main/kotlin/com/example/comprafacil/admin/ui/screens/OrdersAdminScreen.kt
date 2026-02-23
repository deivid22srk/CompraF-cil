package com.example.comprafacil.admin.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comprafacil.admin.ui.viewmodel.AdminOrdersViewModel
import com.example.comprafacil.core.data.Order
import com.example.comprafacil.core.data.OrderItem
import kotlinx.coroutines.launch

@Composable
fun OrdersAdminScreen(viewModel: AdminOrdersViewModel = viewModel()) {
    val orders by viewModel.orders.collectAsState()
    val loading by viewModel.loading.collectAsState()

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text("Pedidos Recebidos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(orders) { order ->
                OrderAdminItem(order, viewModel)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun OrderAdminItem(order: Order, viewModel: AdminOrdersViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    var orderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var loadingItems by remember { mutableStateOf(false) }

    val statuses = listOf("pendente", "aceito", "saindo para entrega", "concluído", "cancelado")
    val statusColor = when(order.status) {
        "pendente" -> Color(0xFF9E9E9E)
        "aceito" -> Color(0xFF2196F3)
        "saindo para entrega" -> Color(0xFFFF9800)
        "concluído" -> Color(0xFF4CAF50)
        else -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Status Indicator Bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pedido #${order.id?.takeLast(6)}", fontWeight = FontWeight.Black, color = statusColor, fontSize = 16.sp)
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            (order.status ?: "pendente").uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(order.customer_name ?: "Cliente não identificado", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(4.dp))

                Text(order.location ?: "Sem endereço", color = Color.LightGray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Pagamento: ${order.payment_method.uppercase()}", color = Color(0xFFFFC107), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("WhatsApp: ${order.whatsapp}", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Text("R$ ${order.total_price}", color = Color(0xFFFF9800), fontSize = 22.sp, fontWeight = FontWeight.Black)
                }

                if (showItems) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Itens do Pedido:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                if (loadingItems) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    orderItems.forEach { item ->
                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text("• ${item.quantity}x (ID: ${item.product_id?.takeLast(4)}) - R$ ${item.price_at_time}", color = Color.Gray, fontSize = 12.sp)
                            item.selected_variations?.let { vars ->
                                val varsText = vars.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                                Text("  $varsText", color = Color(0xFFFDCB58), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = {
                            if (!showItems) {
                                scope.launch {
                                    loadingItems = true
                                    try {
                                        orderItems = viewModel.getItems(order.id!!)
                                    } catch (e: Exception) {} finally {
                                        loadingItems = false
                                    }
                                }
                            }
                            showItems = !showItems
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (showItems) "OCULTAR" else "ITENS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (order.latitude != null && order.longitude != null) {
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:${order.latitude},${order.longitude}?q=${order.latitude},${order.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(" MAPA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(" STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.uppercase()) },
                                    onClick = {
                                        expanded = false
                                        scope.launch {
                                            try {
                                                viewModel.updateStatus(order.id!!, status)
                                                Toast.makeText(context, "Status atualizado!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    viewModel.deleteOrder(order.id!!)
                                    Toast.makeText(context, "Pedido excluído", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp).background(Color(0xFFE53935).copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        }
    }
}
