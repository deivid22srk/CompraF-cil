package com.example.comprafacil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            CompraFacilTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Supabase.client.handleDeeplinks(intent)
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sessionStatus by Supabase.client.auth.sessionStatus.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            if (currentRoute == "login" || currentRoute == null) {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        } else if (sessionStatus is SessionStatus.NotAuthenticated) {
            if (currentRoute != "login") {
                navController.navigate("login") {
                    popUpTo(0)
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute == "home" || currentRoute?.startsWith("product_detail") == true) {
                CustomBottomNavigation()
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "login", modifier = Modifier.padding(padding)) {
            composable("login") { LoginScreen() }
            composable("home") {
                HomeScreen(onProductClick = { id -> navController.navigate("product_detail/$id") })
            }
            composable("product_detail/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")?.toLongOrNull()
                ProductDetailScreen(productId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF2D3B87), Color(0xFF1A237E)))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "CompraFácil",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    "Sua loja em qualquer lugar",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            if (isSignUp) "Criar Conta" else "Bem-vindo",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3B87)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        try {
                                            if (isSignUp) {
                                                Supabase.client.auth.signUpWith(Email) {
                                                    this.email = email
                                                    this.password = password
                                                }
                                            } else {
                                                Supabase.client.auth.signInWith(Email) {
                                                    this.email = email
                                                    this.password = password
                                                }
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar(e.localizedMessage ?: "Erro")
                                        } finally {
                                            loading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3B87))
                            ) {
                                Text(if (isSignUp) "Cadastrar" else "Entrar", fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = { isSignUp = !isSignUp },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(if (isSignUp) "Já tem conta? Entrar" else "Não tem conta? Cadastre-se")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onProductClick: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                products = Supabase.client.postgrest["products"].select().decodeList<Product>()
                categories = Supabase.client.postgrest["categories"].select().decodeList<Category>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FB))) {
        // Custom Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text("O que você está procurando?", color = Color.Gray)
            }
        }

        // Categories
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                CategoryItem(category)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Promo Banner
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFDCB58), Color(0xFFF9A825))))
        ) {
            Row(modifier = Modifier.padding(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Oferta Especial", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.White)
                    }
                    Text("Desconto de 50%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Em toda a loja!", color = Color.White.copy(alpha = 0.9f))
                }
                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.2f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Products Grid
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2D3B87))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { product ->
                    ProductCard(product, onClick = { product.id?.let { onProductClick(it) } })
                }
            }
        }
    }
}

@Composable
fun CategoryItem(category: Category) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val nameLower = category.name.lowercase()
            val icon = when {
                nameLower.contains("celular") || nameLower.contains("smartphone") -> Icons.Default.Devices
                nameLower.contains("roupa") || nameLower.contains("vestuario") -> Icons.Default.Checkroom
                nameLower.contains("esporte") -> Icons.Default.DirectionsRun
                nameLower.contains("relogio") -> Icons.Default.Watch
                else -> Icons.Default.Storefront
            }
            Icon(icon, contentDescription = null, tint = Color(0xFF2D3B87))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(category.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = product.image_url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R$", fontSize = 12.sp, color = Color(0xFF2D3B87), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(String.format("%.2f", product.price), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF2D3B87))
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(productId: Long?, onBack: () -> Unit) {
    var product by remember { mutableStateOf<Product?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(productId) {
        if (productId != null) {
            try {
                product = Supabase.client.postgrest["products"]
                    .select {
                        filter {
                            eq("id", productId)
                        }
                    }
                    .decodeSingle<Product>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        product?.let { p ->
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                Box {
                    AsyncImage(
                        model = p.image_url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(350.dp),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(16.dp).background(Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(p.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("R$ ${String.format("%.2f", p.price)}", fontSize = 24.sp, color = Color(0xFF2D3B87), fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Descrição", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(p.description, color = Color.Gray, lineHeight = 22.sp)

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { /* Add to cart */ },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3B87))
                    ) {
                        Text("Adicionar ao Carrinho", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF2D3B87))
            Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.Gray)

            // Central highlighted button
            Surface(
                modifier = Modifier.size(56.dp).offset(y = (-20).dp),
                shape = CircleShape,
                color = Color(0xFF2D3B87),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                }
            }

            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
        }
    }
}
