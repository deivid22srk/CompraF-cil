package com.example.comprafacil.admin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.comprafacil.SupabaseConfig
import com.example.comprafacil.admin.data.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdminTheme {
                AdminPanel()
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
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Novo") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Produtos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    label = { Text("Pedidos") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> AddProductScreen()
                1 -> ProductListScreen()
                2 -> OrdersAdminScreen()
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
        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Preço (ex: 29.90)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "Selecione uma Categoria",
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoria") },
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        }
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

                            val productData = mapOf(
                                "name" to name,
                                "description" to description,
                                "price" to price.toDouble(),
                                "category_id" to selectedCategory?.id,
                                "image_url" to (imageUrls.firstOrNull() ?: "")
                            )

                            val insertedProduct = SupabaseConfig.client.from("products").insert(productData) {
                                select()
                            }.decodeSingle<Product>()

                            val productId = insertedProduct.id!!

                            if (imageUrls.isNotEmpty()) {
                                val imagesToInsert = imageUrls.map { url ->
                                    mapOf("product_id" to productId, "image_url" to url)
                                }
                                SupabaseConfig.client.from("product_images").insert(imagesToInsert)
                            }

                            Toast.makeText(context, "Produto adicionado!", Toast.LENGTH_LONG).show()
                            name = ""; description = ""; price = ""; selectedCategory = null; selectedImages = emptyList()
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pedido #${order.id?.takeLast(6)}", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Text(order.status ?: "Pendente", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total: R$ ${order.total_price}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Endereço: ${order.location}", color = Color.White)
            Text("WhatsApp: ${order.whatsapp}", color = Color.White)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Text(" MAPA", fontSize = 10.sp)
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val nextStatus = when(order.status) {
                                    "pendente" -> "em rota"
                                    "em rota" -> "entregue"
                                    else -> "cancelado"
                                }
                                SupabaseConfig.client.from("orders").update(
                                    {
                                        set("status", nextStatus)
                                    }
                                ) {
                                    filter { eq("id", order.id!!) }
                                }
                                onUpdate()
                                Toast.makeText(context, "Status atualizado!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(" STATUS", fontSize = 10.sp)
                }
            }
        }
    }
}
