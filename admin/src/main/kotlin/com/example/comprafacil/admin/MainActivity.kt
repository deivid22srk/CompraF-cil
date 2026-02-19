package com.example.comprafacil.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.comprafacil.admin.data.Product
import com.example.comprafacil.admin.data.SupabaseConfig
import com.example.comprafacil.admin.ui.theme.CompraFacilAdminTheme
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompraFacilAdminTheme {
                AdminApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApp() {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun fetchProducts() {
        isLoading = true
        scope.launch {
            try {
                products = SupabaseConfig.client.postgrest["products"]
                    .select().decodeList<Product>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchProducts()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Painel Admin") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    ProductItem(product, onDelete = {
                        scope.launch {
                            try {
                                SupabaseConfig.client.postgrest["products"].delete {
                                    filter {
                                        eq("id", product.id!!)
                                    }
                                }
                                fetchProducts()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    })
                }
            }
        }

        if (showAddDialog) {
            AddProductDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, price, description, imageUrl ->
                    scope.launch {
                        try {
                            val newProduct = Product(
                                name = name,
                                price = price.toDoubleOrNull() ?: 0.0,
                                description = description,
                                image_url = imageUrl
                            )
                            SupabaseConfig.client.postgrest["products"].insert(newProduct)
                            fetchProducts()
                            showAddDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProductItem(product: Product, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleLarge)
                Text("R$ ${String.format("%.2f", product.price)}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddProductDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Produto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
                TextField(value = price, onValueChange = { price = it }, label = { Text("Preço") })
                TextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") })
                TextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL da Imagem") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, price, description, imageUrl) }) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
