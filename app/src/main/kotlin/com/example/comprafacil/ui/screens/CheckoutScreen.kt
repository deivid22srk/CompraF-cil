package com.example.comprafacil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.comprafacil.data.CartItem
import com.example.comprafacil.data.Order
import com.example.comprafacil.data.SupabaseConfig
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(cartItems: List<CartItem>, total: Double, onBack: () -> Unit, onOrderConfirmed: () -> Unit) {
    val scope = rememberCoroutineScope()
    var whatsapp by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Detalhes da Entrega", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("Número do WhatsApp") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Sua Localização Exata") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Resumo do Pedido", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    cartItems.forEach { item ->
                        Row {
                            Text("${item.quantity}x ${item.product?.name}")
                            Spacer(modifier = Modifier.weight(1f))
                            Text("R$ ${String.format("%.2f", (item.product?.price ?: 0.0) * item.quantity)}")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("R$ ${String.format("%.2f", total)}", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (whatsapp.isBlank() || location.isBlank()) return@Button
                    isProcessing = true
                    scope.launch {
                        try {
                            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                            val order = Order(
                                user_id = userId,
                                whatsapp = whatsapp,
                                location = location,
                                total_price = total
                            )
                            SupabaseConfig.client.postgrest["orders"].insert(order)

                            // Clear cart
                            if (userId != null) {
                                SupabaseConfig.client.postgrest["cart_items"].delete { filter { eq("user_id", userId) } }
                            }

                            onOrderConfirmed()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isProcessing
            ) {
                if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Confirmar Pedido")
            }
        }
    }
}
