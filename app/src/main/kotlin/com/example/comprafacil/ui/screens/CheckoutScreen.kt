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
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    productId: String? = null,
    quantity: Int = 1,
    variationsJson: String? = null,
    onBack: () -> Unit,
    onOrderFinished: () -> Unit
) {
    val client = SupabaseConfig.client
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var customerName by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var paymentMethod by remember { mutableStateOf("dinheiro") }
    var subtotal by remember { mutableDoubleStateOf(0.0) }
    var deliveryFee by remember { mutableDoubleStateOf(0.0) }
    var loading by remember { mutableStateOf(true) }
    var fetchingLocation by remember { mutableStateOf(false) }
    var isPlacingOrder by remember { mutableStateOf(false) }

    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var expandedAddress by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf<Address?>(null) }

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

    val selectedVariations = remember(variationsJson) {
        if (variationsJson != null) {
            try {
                // Decode from the encoded JSON string passed in the URL
                kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(Uri.decode(variationsJson))
            } catch (e: Exception) {
                null
            }
        } else null
    }

    LaunchedEffect(Unit) {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                if (productId != null) {
                    val product = client.from("products").select {
                        filter { eq("id", productId) }
                    }.decodeSingle<Product>()
                    subtotal = product.price * quantity
                } else {
                    val items = client.from("cart_items").select(Columns.raw("*, product:products(*)")) {
                        filter { eq("user_id", userId) }
                    }.decodeAs<List<CartItem>>()
                    subtotal = items.sumOf { (it.product?.price ?: 0.0) * it.quantity }
                }

                val configs = client.from("app_config").select().decodeAs<List<AppConfig>>()
                configs.find { it.key == "delivery_fee" }?.let {
                    deliveryFee = it.value.jsonPrimitive.doubleOrNull ?: 0.0
                }

                val profile = client.from("profiles").select { filter { eq("id", userId) } }.decodeSingleOrNull<Profile>()
                customerName = profile?.full_name ?: ""
                whatsapp = profile?.whatsapp ?: ""

                addresses = client.from("addresses").select { filter { eq("user_id", userId) } }.decodeAs<List<Address>>()
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
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Seu Nome") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (addresses.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expandedAddress,
                    onExpandedChange = { expandedAddress = !expandedAddress },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedAddress?.name ?: "Selecione um Endereço Salvo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Endereço de Entrega") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAddress) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAddress,
                        onDismissRequest = { expandedAddress = false }
                    ) {
                        addresses.forEach { addr ->
                            DropdownMenuItem(
                                text = { Text("${addr.name} - ${addr.address_line}") },
                                onClick = {
                                    selectedAddress = addr
                                    locationName = addr.address_line
                                    whatsapp = addr.phone
                                    latitude = addr.latitude
                                    longitude = addr.longitude
                                    expandedAddress = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ou preencha um novo abaixo:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }

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

            OutlinedButton(
                onClick = {
                    val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    if (fineLocation == PackageManager.PERMISSION_GRANTED) {
                        getCurrentLocation()
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = MaterialTheme.colorScheme.onSurface)
                        Text("R$ ${String.format("%.2f", subtotal)}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Taxa de Entrega", color = MaterialTheme.colorScheme.onSurface)
                        Text("R$ ${String.format("%.2f", deliveryFee)}")
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total a pagar:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("R$ ${String.format("%.2f", subtotal + deliveryFee)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isPlacingOrder) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        if (customerName.isBlank() || whatsapp.isBlank() || locationName.isBlank() || latitude == null) {
                            Toast.makeText(context, "Preencha seu nome, endereço e capture sua localização", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        scope.launch {
                            isPlacingOrder = true
                            val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                            try {
                                val orderItemsList = if (productId != null) {
                                    val product = client.from("products").select {
                                        filter { eq("id", productId) }
                                    }.decodeSingle<Product>()

                                    listOf(
                                        OrderItem(
                                            order_id = "", // Will be set after order insertion
                                            product_id = productId,
                                            quantity = quantity,
                                            price_at_time = product.price,
                                            selected_variations = selectedVariations
                                        )
                                    )
                                } else {
                                    val cartItems = client.from("cart_items").select(Columns.raw("*, product:products(*)")) {
                                        filter { eq("user_id", userId) }
                                    }.decodeAs<List<CartItem>>()

                                    if (cartItems.isEmpty()) {
                                        Toast.makeText(context, "Seu carrinho está vazio", Toast.LENGTH_SHORT).show()
                                        isPlacingOrder = false
                                        return@launch
                                    }

                                    cartItems.map { cartItem ->
                                        OrderItem(
                                            order_id = "",
                                            product_id = cartItem.product_id,
                                            quantity = cartItem.quantity,
                                            price_at_time = cartItem.product?.price ?: 0.0,
                                            selected_variations = cartItem.selected_variations
                                        )
                                    }
                                }

                                val order = Order(
                                    user_id = userId,
                                    customer_name = customerName,
                                    whatsapp = whatsapp,
                                    location = locationName,
                                    total_price = subtotal + deliveryFee,
                                    latitude = latitude,
                                    longitude = longitude,
                                    payment_method = paymentMethod,
                                    status = "pendente"
                                )

                                val insertedOrder = client.from("orders").insert(order) {
                                    select()
                                }.decodeSingle<Order>()

                                val orderId = insertedOrder.id!!

                                // Update order items with the new order ID
                                val finalOrderItems = orderItemsList.map { it.copy(order_id = orderId) }
                                client.from("order_items").insert(finalOrderItems)

                                // Clear cart only if it was a cart purchase
                                if (productId == null) {
                                    client.from("cart_items").delete {
                                        filter { eq("user_id", userId) }
                                    }
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
