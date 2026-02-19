package com.example.comprafacil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.comprafacil.data.CartItem
import com.example.comprafacil.data.Product
import com.example.comprafacil.data.SupabaseConfig
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(onBack: () -> Unit, onCheckout: (List<CartItem>, Double) -> Unit) {
    val scope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchCart() {
        scope.launch {
            try {
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                cartItems = SupabaseConfig.client.postgrest["cart_items"]
                    .select {
                        filter { eq("user_id", userId) }
                    }.decodeList<CartItem>()

                // For each cart item, fetch the product if not included
                // Ideally use join in select, but for simplicity:
                cartItems = cartItems.map { item ->
                    val product = SupabaseConfig.client.postgrest["products"]
                        .select { filter { eq("id", item.product_id) } }.decodeSingle<Product>()
                    item.copy(product = product)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchCart()
    }

    val total = cartItems.sumOf { (it.product?.price ?: 0.0) * it.quantity }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Meu Carrinho") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(tonalElevation = 8.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Text("Total", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("R$ ${String.format("%.2f", total)}", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onCheckout(cartItems, total) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ir para Checkout")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (cartItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Seu carrinho está vazio.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(cartItems) { item ->
                    CartRow(item, onRemove = {
                        scope.launch {
                            SupabaseConfig.client.postgrest["cart_items"].delete { filter { eq("id", item.id!!) } }
                            fetchCart()
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun CartRow(item: CartItem, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.product?.images?.firstOrNull()?.image_url ?: "https://via.placeholder.com/80",
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product?.name ?: "Produto", fontWeight = FontWeight.Bold)
                Text("R$ ${String.format("%.2f", item.product?.price ?: 0.0)}", color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Qtd: ${item.quantity}", fontSize = 14.sp)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}
