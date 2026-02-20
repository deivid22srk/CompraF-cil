package com.example.comprafacil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.example.comprafacil.SupabaseConfig
import com.example.comprafacil.data.CartItem
import com.example.comprafacil.data.Product
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(onCheckout: () -> Unit) {
    val client = SupabaseConfig.client
    val scope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                // Fetch cart items with product details in a single query (Join)
                cartItems = client.from("cart_items").select(Columns.raw("*, product:products(*)")) {
                    filter { eq("user_id", userId) }
                }.decodeAs<List<CartItem>>()
            } catch (e: Exception) {
                // handle error
            }
        }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Carrinho", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (cartItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Text("Seu carrinho está vazio", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                    items(cartItems) { item ->
                        item.product?.let { product ->
                            CartItemRow(
                                product = product,
                                quantity = item.quantity,
                                onUpdateQuantity = { newQuantity ->
                                    if (newQuantity <= 0) {
                                        scope.launch {
                                            client.from("cart_items").delete {
                                                filter { eq("id", item.id!!) }
                                            }
                                            cartItems = cartItems.filter { it.id != item.id }
                                        }
                                    } else if (newQuantity <= (product.stock_quantity ?: 0)) {
                                        scope.launch {
                                            client.from("cart_items").update({
                                                set("quantity", newQuantity)
                                            }) {
                                                filter { eq("id", item.id!!) }
                                            }
                                            cartItems = cartItems.map {
                                                if (it.id == item.id) it.copy(quantity = newQuantity) else it
                                            }
                                        }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        client.from("cart_items").delete {
                                            filter { eq("id", item.id!!) }
                                        }
                                        cartItems = cartItems.filter { it.id != item.id }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                val total = cartItems.sumOf { (it.product?.price ?: 0.0) * it.quantity }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("R$ ${String.format("%.2f", total)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("CONTINUAR PARA PAGAMENTO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(product: Product, quantity: Int, onUpdateQuantity: (Int) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.image_url ?: product.images?.firstOrNull()?.image_url ?: "",
                contentDescription = null,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("R$ ${String.format("%.2f", product.price)} cada", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateQuantity(quantity - 1) },
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text("$quantity", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { onUpdateQuantity(quantity + 1) },
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}
