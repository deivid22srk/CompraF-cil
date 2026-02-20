package com.example.comprafacil.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import com.example.comprafacil.SupabaseConfig
import com.example.comprafacil.data.CartItem
import com.example.comprafacil.data.Order
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(onBack: () -> Unit, onOrderFinished: () -> Unit) {
    val client = SupabaseConfig.client
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var whatsapp by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var paymentMethod by remember { mutableStateOf("dinheiro") }
    var total by remember { mutableDoubleStateOf(0.0) }
    var loading by remember { mutableStateOf(true) }
    var fetchingLocation by remember { mutableStateOf(false) }
    var isPlacingOrder by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            // Permission granted
        } else {
            Toast.makeText(context, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

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

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        scope.launch {
            fetchingLocation = true
            try {
                val priority = Priority.PRIORITY_HIGH_ACCURACY
                val result = fusedLocationClient.getCurrentLocation(priority, CancellationTokenSource().token).await()
                if (result != null) {
                    latitude = result.latitude
                    longitude = result.longitude
                    Toast.makeText(context, "Localização capturada!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Não foi possível obter a localização", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao obter localização: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                fetchingLocation = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Finalizar Pedido") },
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
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Estamos quase lá!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Apenas para Sítio Riacho dos Barreiros e locais próximos.", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Precisamos da sua localização exata para a entrega", color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Endereço (Rua, Número, Bairro)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    if (fineLocation == PackageManager.PERMISSION_GRANTED) {
                        getCurrentLocation()
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (fetchingLocation) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (latitude != null) "LOCALIZAÇÃO CAPTURADA" else "OBTER LOCALIZAÇÃO EXATA")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Forma de Pagamento", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paymentMethod == "dinheiro",
                    onClick = { paymentMethod = "dinheiro" },
                    label = { Text("Dinheiro") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = paymentMethod == "pix",
                    onClick = { paymentMethod = "pix" },
                    label = { Text("Pix") },
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Pagamento realizado no ato da entrega.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Total a pagar:", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    Text("R$ ${String.format("%.2f", total)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isPlacingOrder) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        if (whatsapp.isBlank() || locationName.isBlank() || latitude == null) {
                            Toast.makeText(context, "Preencha tudo e capture sua localização", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        scope.launch {
                            isPlacingOrder = true
                            val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                            try {
                                val order = Order(
                                    user_id = userId,
                                    whatsapp = whatsapp,
                                    location = locationName,
                                    total_price = total,
                                    latitude = latitude,
                                    longitude = longitude,
                                    payment_method = paymentMethod,
                                    status = "pendente"
                                )

                                client.from("orders").insert(order)

                                client.from("cart_items").delete {
                                    filter { eq("user_id", userId) }
                                }

                                Toast.makeText(context, "Pedido realizado com sucesso!", Toast.LENGTH_LONG).show()
                                onOrderFinished()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isPlacingOrder = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("REALIZAR PEDIDO", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
