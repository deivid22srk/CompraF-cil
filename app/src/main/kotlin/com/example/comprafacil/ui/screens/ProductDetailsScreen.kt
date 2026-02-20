package com.example.comprafacil.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.comprafacil.data.Product
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailsScreen(productId: String, onBack: () -> Unit) {
    val client = SupabaseConfig.client
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var product by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(productId) {
        try {
            product = client.from("products").select(io.github.jan.supabase.postgrest.query.Columns.raw("*, images:product_images(*)")) {
                filter { eq("id", productId) }
            }.decodeSingle<Product>()
        } catch (e: Exception) {
            // handle error
        } finally {
            loading = false
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
    } else if (product != null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (quantity > 1) quantity-- }) {
                                Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("$quantity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { quantity++ }) {
                                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val userId = client.auth.currentUserOrNull()?.id
                                    if (userId != null) {
                                        client.from("cart_items").insert(
                                            mapOf("user_id" to userId, "product_id" to productId, "quantity" to quantity)
                                        )
                                        Toast.makeText(context, "Adicionado ao carrinho!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Faça login primeiro", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("ADICIONAR R$ ${String.format("%.2f", product!!.price * quantity)}", color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                Box {
                    val pagerState = rememberPagerState(pageCount = { product!!.images?.size ?: 1 })
                    HorizontalPager(state = pagerState, modifier = Modifier.height(300.dp)) { page ->
                        AsyncImage(
                            model = product!!.images?.getOrNull(page)?.image_url ?: "",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(product!!.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("R$ ${String.format("%.2f", product!!.price)}", fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Descrição", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(product!!.description ?: "Sem descrição disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
