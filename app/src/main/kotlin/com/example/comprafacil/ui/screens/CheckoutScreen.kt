package com.example.comprafacil.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comprafacil.SupabaseConfig
import com.example.comprafacil.data.CartItem
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(onBack: () -> Unit, onOrderFinished: () -> Unit) {
    val client = SupabaseConfig.client
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var whatsapp by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var total by remember { mutableDoubleStateOf(0.0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                val items = client.from("cart_items").select(Columns.raw("*, product:products(*)")) {
                    filter { eq("user_id", userId) }
                }.decodeAs<List<CartItem>>()

                total = items.sumOf { (it.product?.price ?: 0.0) * it.quantity }
            } catch (e: Exception) { /* ... */ }
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finalizar Pedido") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Estamos quase lá!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Informe seus dados para entrega via WhatsApp", color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp (com DDD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Endereço de Entrega") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDCB58).copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Total a pagar:", modifier = Modifier.weight(1f))
                    Text("R$ ${String.format("%.2f", total)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (whatsapp.isBlank() || location.isBlank()) {
                        Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                        try {
                            // Create Order
                            client.from("orders").insert(
                                mapOf("user_id" to userId, "whatsapp" to whatsapp, "location" to location, "total_price" to total)
                            )
                            // Clear Cart
                            client.from("cart_items").delete {
                                filter { eq("user_id", userId) }
                            }

                            // Redirect to WhatsApp
                            val message = "Olá! Gostaria de finalizar meu pedido de R$ ${String.format("%.2f", total)}. Entrega em: $location"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=55${whatsapp}&text=${Uri.encode(message)}"))
                            context.startActivity(intent)

                            onOrderFinished()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green
            ) {
                Text("FINALIZAR NO WHATSAPP", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
