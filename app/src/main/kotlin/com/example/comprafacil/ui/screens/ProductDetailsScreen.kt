package com.example.comprafacil.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.CartItem
import com.example.comprafacil.core.data.Product
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
    var showZoomDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }

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
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .padding(horizontal = 4.dp)
                        ) {
                            IconButton(onClick = { if (quantity > 1) quantity-- }) {
                                Text("-", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                            Text("$quantity", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(
                                onClick = {
                                    if (quantity < (product?.stock_quantity ?: 0)) {
                                        quantity++
                                    } else {
                                        Toast.makeText(context, "Limite de estoque atingido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                if ((product?.stock_quantity ?: 0) <= 0) {
                                    Toast.makeText(context, "Produto sem estoque", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val userId = client.auth.currentUserOrNull()?.id
                                    if (userId != null) {
                                        try {
                                            // Check if already in cart
                                            val existing = client.from("cart_items").select {
                                                filter {
                                                    eq("user_id", userId)
                                                    eq("product_id", productId)
                                                }
                                            }.decodeSingleOrNull<CartItem>()

                                            if (existing != null) {
                                                val newQuantity = existing.quantity + quantity
                                                if (newQuantity > (product?.stock_quantity ?: 0)) {
                                                    Toast.makeText(context, "Quantidade total no carrinho excede o estoque", Toast.LENGTH_LONG).show()
                                                } else {
                                                    client.from("cart_items").update({
                                                        set("quantity", newQuantity)
                                                    }) {
                                                        filter { eq("id", existing.id!!) }
                                                    }
                                                    Toast.makeText(context, "Carrinho atualizado!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                val cartItem = CartItem(
                                                    user_id = userId,
                                                    product_id = productId,
                                                    quantity = quantity
                                                )
                                                client.from("cart_items").insert(cartItem)
                                                Toast.makeText(context, "Adicionado ao carrinho!", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao adicionar: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Faça login primeiro", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                "ADICIONAR R$ ${String.format("%.2f", product!!.price * quantity)}",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                Box {
                    val pagerState = rememberPagerState(pageCount = { product!!.images?.size ?: 1 })
                    HorizontalPager(state = pagerState, modifier = Modifier.height(350.dp)) { page ->
                        val imageUrl = product!!.images?.getOrNull(page)?.image_url ?: ""
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    selectedImageUrl = imageUrl
                                    showZoomDialog = true
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val shareUrl = "https://comprafacil.ct.ws/#/product/${product!!.id}"
                                putExtra(Intent.EXTRA_SUBJECT, product!!.name)
                                putExtra(Intent.EXTRA_TEXT, "Confira este produto no CompraFácil: ${product!!.name}\n\n$shareUrl")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Produto"))
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }

                    // Pager indicator
                    if ((product!!.images?.size ?: 0) > 1) {
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(product!!.images!!.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            product!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        if ((product!!.stock_quantity ?: 0) > 0) {
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "ESTOQUE: ${product!!.stock_quantity}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Vendido por: ${product!!.sold_by ?: "CompraFácil"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "R$ ${String.format("%.2f", product!!.price)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Descrição",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        product!!.description ?: "Sem descrição disponível.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showZoomDialog) {
        ZoomDialog(
            imageUrl = selectedImageUrl,
            onDismiss = { showZoomDialog = false }
        )
    }
}

@Composable
fun ZoomDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ZoomableImage(
                imageUrl = imageUrl,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
            }
        }
    }
}

@Composable
fun ZoomableImage(imageUrl: String, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .clip(androidx.compose.ui.graphics.RectangleShape)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    scale = scale.coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offset += pan
                    } else {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                }
            }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}
