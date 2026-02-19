package com.example.comprafaciladmin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            CompraFacilAdminTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AdminNavigation()
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
fun AdminNavigation() {
    val navController = rememberNavController()
    val sessionStatus by Supabase.client.auth.sessionStatus.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            if (currentRoute == "login" || currentRoute == null) {
                navController.navigate("dashboard") {
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

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            AdminLoginScreen()
        }
        composable("dashboard") {
            DashboardScreen(
                onAddProduct = { navController.navigate("add_product") },
                onLogout = { Supabase.client.auth.signOut() }
            )
        }
        composable("add_product") {
            AddProductScreen(
                onProductAdded = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun AdminLoginScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFF2D3B87)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF2D3B87)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Painel Admin",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2D3B87)
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Admin") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (loading) {
                    CircularProgressIndicator(color = Color(0xFF2D3B87))
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                loading = true
                                try {
                                    Supabase.client.auth.signInWith(Email) {
                                        this.email = email
                                        this.password = password
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Erro: ${e.localizedMessage}")
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3B87))
                    ) {
                        Text("Entrar no Painel", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onAddProduct: () -> Unit, onLogout: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                products = Supabase.client.postgrest["products"].select().decodeList<Product>()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gerenciamento", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { scope.launch { onLogout() } }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF2D3B87)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Novo Produto") },
                containerColor = Color(0xFF2D3B87),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2D3B87))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).background(Color(0xFFF5F7FB)).fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { product ->
                    AdminProductCard(product)
                }
            }
        }
    }
}

@Composable
fun AdminProductCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.image_url,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F7FB)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("R$ ${String.format("%.2f", product.price)}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2D3B87), fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { /* Edit */ }) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF2D3B87))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(onProductAdded: () -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                categories = Supabase.client.postgrest["categories"].select().decodeList<Category>()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Produto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5F7FB))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Text("Selecionar Foto", color = Color.Gray)
                    }
                } else {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Produto") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Selecionar Categoria",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
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
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Preço (R$)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (uploading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF2D3B87))
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            uploading = true
                            try {
                                var imageUrl: String? = null
                                if (imageUri != null) {
                                    val inputStream = context.contentResolver.openInputStream(imageUri!!)
                                    val bytes = inputStream?.readBytes()
                                    if (bytes != null) {
                                        val fileName = "${UUID.randomUUID()}.jpg"
                                        val bucket = Supabase.client.storage["product-images"]
                                        bucket.upload(fileName, bytes)
                                        imageUrl = bucket.publicUrl(fileName)
                                    }
                                }

                                val product = Product(
                                    name = name,
                                    description = description,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    image_url = imageUrl,
                                    category_id = selectedCategory?.id
                                )
                                Supabase.client.postgrest["products"].insert(product)
                                onProductAdded()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                uploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    enabled = name.isNotBlank() && price.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3B87))
                ) {
                    Text("Salvar Produto", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
