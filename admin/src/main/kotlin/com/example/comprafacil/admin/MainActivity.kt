package com.example.comprafacil.admin

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.comprafacil.SupabaseConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Produtos") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> AddProductScreen()
                1 -> ProductListScreen()
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
    var categoryId by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploading by remember { mutableStateOf(false) }

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
        OutlinedTextField(value = categoryId, onValueChange = { categoryId = it }, label = { Text("ID da Categoria (UUID)") }, modifier = Modifier.fillMaxWidth())

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

                            val product = mapOf(
                                "name" to name,
                                "description" to description,
                                "price" to price.toDouble(),
                                "category_id" to if (categoryId.isBlank()) null else categoryId,
                                "image_url" to (imageUrls.firstOrNull() ?: "")
                            )

                            val result = SupabaseConfig.client.from("products").insert(product) {
                                select()
                            }.decodeSingle<Map<String, Any>>()

                            val productId = result["id"] as String

                            if (imageUrls.isNotEmpty()) {
                                val imagesToInsert = imageUrls.map { url ->
                                    mapOf("product_id" to productId, "image_url" to url)
                                }
                                SupabaseConfig.client.from("product_images").insert(imagesToInsert)
                            }

                            Toast.makeText(context, "Produto adicionado!", Toast.LENGTH_LONG).show()
                            name = ""; description = ""; price = ""; categoryId = ""; selectedImages = emptyList()
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
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            products = SupabaseConfig.client.from("products").select().decodeAs<List<Map<String, Any>>>()
        } catch (e: Exception) {
            // handle error
        } finally {
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
                    products = products.filter { it["id"] != product["id"] }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProductAdminItem(product: Map<String, Any>, onDelete: () -> Unit) {
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
                model = product["image_url"] as? String,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product["name"] as String, fontWeight = FontWeight.Bold, color = Color.White)
                Text("R$ ${product["price"]}", color = Color(0xFFFF9800))
            }
            IconButton(onClick = {
                scope.launch {
                    try {
                        SupabaseConfig.client.from("products").delete {
                            filter {
                                eq("id", product["id"] as String)
                            }
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
