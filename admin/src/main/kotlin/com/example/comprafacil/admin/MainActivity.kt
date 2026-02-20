package com.example.comprafacil.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.comprafacil.SupabaseConfig
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.comprafacil.admin.data.*
import com.example.comprafacil.admin.utils.NotificationHelper
import com.example.comprafacil.admin.utils.NewOrderWorker
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseConfig.initialize(this)

        val notificationHelper = NotificationHelper(this)
        val workRequest = PeriodicWorkRequestBuilder<NewOrderWorker>(15, java.util.concurrent.TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("admin_orders", ExistingPeriodicWorkPolicy.KEEP, workRequest)

        setContent {
            AdminTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Realtime Listener for New Orders
                    try {
                        val channel = SupabaseConfig.client.realtime.channel("admin_orders")
                        val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                            this.table = "orders"
                        }
                        insertFlow.onEach { change ->
                            val customerName = change.record["customer_name"]?.jsonPrimitive?.content ?: "Cliente"
                            val orderId = change.record["id"]?.jsonPrimitive?.content?.takeLast(6) ?: ""
                            notificationHelper.showNotification(
                                "Novo Pedido Recebido!",
                                "Pedido #$orderId de $customerName"
                            )
                        }.launchIn(this)
                        channel.subscribe()
                    } catch (e: Exception) {}
                }

                AdminPanel()
            }
        }
    }
}

@Composable
fun CategoryAdminScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var newCategoryName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    fun fetchCategories() {
        scope.launch {
            try {
                categories = SupabaseConfig.client.from("categories").select().decodeAs<List<Category>>()
            } catch (e: Exception) {} finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchCategories()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Categorias", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("Nova Categoria") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newCategoryName.isNotBlank()) {
                    scope.launch {
                        try {
                            SupabaseConfig.client.from("categories").insert(Category(name = newCategoryName))
                            newCategoryName = ""
                            fetchCategories()
                            Toast.makeText(context, "Adicionada!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF9800))
            }
        } else {
            LazyColumn {
                items(categories) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(category.name, modifier = Modifier.weight(1f), color = Color.White)
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        SupabaseConfig.client.from("categories").delete {
                                            filter { eq("id", category.id!!) }
                                        }
                                        fetchCategories()
                                        Toast.makeText(context, "Excluída", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAdminScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var minVersion by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var deliveryFee by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val configs = SupabaseConfig.client.from("app_config").select().decodeAs<List<AppConfig>>()
            configs.forEach { config ->
                when (config.key) {
                    "min_version" -> minVersion = config.value.jsonPrimitive.content
                    "download_url" -> downloadUrl = config.value.jsonPrimitive.content
                    "delivery_fee" -> deliveryFee = config.value.jsonPrimitive.content
                }
            }
        } catch (e: Exception) {} finally {
            loading = false
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Configurações Globais", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(value = minVersion, onValueChange = { minVersion = it }, label = { Text("Versão Mínima do App (ex: 1.1)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = downloadUrl, onValueChange = { downloadUrl = it }, label = { Text("URL de Download do APK") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("Taxa de Entrega (R$)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val updates = listOf(
                                AppConfig("min_version", JsonPrimitive(minVersion)),
                                AppConfig("download_url", JsonPrimitive(downloadUrl)),
                                AppConfig("delivery_fee", JsonPrimitive(deliveryFee))
                            )
                            SupabaseConfig.client.from("app_config").upsert(updates)
                            Toast.makeText(context, "Configurações salvas!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("SALVAR CONFIGURAÇÕES", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF9800),
            secondary = Color(0xFFFDCB58),
            surface = Color(0xFF121212),
            background = Color(0xFF121212),
            onPrimary = Color.White
        ),
        content = content
    )
}

@Composable
fun AdminPanel() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Novo") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Produtos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    label = { Text("Pedidos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    label = { Text("Categorias") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Config") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> AddProductScreen()
                1 -> ProductListScreen()
                2 -> OrdersAdminScreen()
                3 -> CategoryAdminScreen()
                4 -> SettingsAdminScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var soldBy by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            categories = SupabaseConfig.client.from("categories").select().decodeAs<List<Category>>()
        } catch (e: Exception) {}
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        selectedImages = uris
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Adicionar Produto", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Produto") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Preço (ex: 29.90)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = stockQuantity, onValueChange = { stockQuantity = it }, label = { Text("Estoque Disponível") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = soldBy, onValueChange = { soldBy = it }, label = { Text("Vendido por (Nome da Loja/Vendedor)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "Selecione uma Categoria",
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoria") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Selecionar Imagens (${selectedImages.size})")
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(selectedImages) { uri ->
                Text(uri.toString(), fontSize = 10.sp, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uploading) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
        } else {
            Button(
                onClick = {
                    if (name.isBlank() || price.isBlank()) {
                        Toast.makeText(context, "Nome e preço são obrigatórios", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        uploading = true
                        try {
                            val imageUrls = mutableListOf<String>()
                            for (uri in selectedImages) {
                                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                if (bytes != null) {
                                    val fileName = "products/${UUID.randomUUID()}.jpg"
                                    val bucket = SupabaseConfig.client.storage.from("product-images")
                                    bucket.upload(fileName, bytes)
                                    imageUrls.add(bucket.publicUrl(fileName))
                                }
                            }

                            val productData = Product(
                                name = name,
                                description = description,
                                price = price.toDoubleOrNull() ?: 0.0,
                                stock_quantity = stockQuantity.toIntOrNull() ?: 0,
                                sold_by = soldBy.ifBlank { null },
                                category_id = selectedCategory?.id,
                                image_url = imageUrls.firstOrNull() ?: ""
                            )

                            val insertedProduct = SupabaseConfig.client.from("products").insert(productData) {
                                select()
                            }.decodeSingle<Product>()

                            val productId = insertedProduct.id!!

                            if (imageUrls.isNotEmpty()) {
                                val imagesToInsert = imageUrls.map { url ->
                                    ProductImage(product_id = productId, image_url = url)
                                }
                                SupabaseConfig.client.from("product_images").insert(imagesToInsert)
                            }

                            Toast.makeText(context, "Produto adicionado!", Toast.LENGTH_LONG).show()
                            name = ""; description = ""; price = ""; stockQuantity = ""; soldBy = ""; selectedCategory = null; selectedImages = emptyList()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            uploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CADASTRAR PRODUTO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProductListScreen() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            products = SupabaseConfig.client.from("products").select().decodeAs<List<Product>>()
        } catch (e: Exception) {} finally {
            loading = false
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text("Produtos Cadastrados", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(products) { product ->
                ProductAdminItem(product) {
                    products = products.filter { it.id != product.id }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProductAdminItem(product: Product, onDelete: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.image_url,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, color = Color.White)
                Text("R$ ${product.price}", color = Color(0xFFFF9800))
                Text("Estoque: ${product.stock_quantity}", color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = {
                scope.launch {
                    try {
                        SupabaseConfig.client.from("products").delete {
                            filter { eq("id", product.id!!) }
                        }
                        onDelete()
                        Toast.makeText(context, "Excluído", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao excluir", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

@Composable
fun OrdersAdminScreen() {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun fetchOrders() {
        scope.launch {
            try {
                orders = SupabaseConfig.client.from("orders").select {
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeAs<List<Order>>()
            } catch (e: Exception) {} finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchOrders()
    }

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
                OrderAdminItem(order) {
                    fetchOrders()
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun OrderAdminItem(order: Order, onUpdate: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("pendente", "aceito", "saindo para entrega", "concluído", "cancelado")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pedido #${order.id?.takeLast(6)}", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Surface(
                    color = when(order.status) {
                        "pendente" -> Color.Gray
                        "aceito" -> Color(0xFF2196F3)
                        "saindo para entrega" -> Color(0xFFFF9800)
                        "concluído" -> Color(0xFF4CAF50)
                        else -> Color.Red
                    }.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        (order.status ?: "pendente").uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when(order.status) {
                            "pendente" -> Color.Gray
                            "aceito" -> Color(0xFF2196F3)
                            "saindo para entrega" -> Color(0xFFFF9800)
                            "concluído" -> Color(0xFF4CAF50)
                            else -> Color.Red
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cliente: ${order.customer_name ?: "Não informado"}", fontWeight = FontWeight.Bold, color = Color.White)
            Text("Total: R$ ${order.total_price}", color = Color(0xFFFF9800), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Endereço: ${order.location}", color = Color.White, fontSize = 14.sp)
            Text("WhatsApp: ${order.whatsapp}", color = Color.White, fontSize = 14.sp)
            Text("Pagamento: ${order.payment_method.uppercase()}", color = Color(0xFFFDCB58), fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (order.latitude != null && order.longitude != null) {
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:${order.latitude},${order.longitude}?q=${order.latitude},${order.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" MAPA", fontSize = 10.sp)
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" STATUS", fontSize = 10.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.uppercase()) },
                                onClick = {
                                    expanded = false
                                    scope.launch {
                                        try {
                                            // Update Order Status
                                            SupabaseConfig.client.from("orders").update(
                                                {
                                                    set("status", status)
                                                }
                                            ) {
                                                filter { eq("id", order.id!!) }
                                            }

                                            // Add to History
                                            val history = OrderStatusHistory(
                                                order_id = order.id!!,
                                                status = status,
                                                notes = "Atualizado pelo administrador"
                                            )
                                            SupabaseConfig.client.from("order_status_history").insert(history)

                                            onUpdate()
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

                IconButton(onClick = {
                    scope.launch {
                        try {
                            SupabaseConfig.client.from("orders").delete {
                                filter { eq("id", order.id!!) }
                            }
                            onUpdate()
                            Toast.makeText(context, "Pedido excluído", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                }
            }
        }
    }
}
