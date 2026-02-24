package com.example.comprafacil.admin

import android.Manifest
import android.app.Activity
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.comprafacil.core.SupabaseConfig
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.comprafacil.core.data.*
import com.example.comprafacil.admin.utils.NotificationHelper
import com.example.comprafacil.admin.utils.NewOrderWorker
import com.example.comprafacil.admin.ui.screens.*
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
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
                val sessionStatus by SupabaseConfig.client.auth.sessionStatus.collectAsState()
                var isAdminLoggedIn by remember { mutableStateOf(false) }

                LaunchedEffect(sessionStatus) {
                    if (sessionStatus is SessionStatus.Authenticated) {
                        val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                        if (userId != null) {
                            try {
                                val profile = SupabaseConfig.client.from("profiles")
                                    .select { filter { eq("id", userId) } }
                                    .decodeSingleOrNull<Profile>()
                                isAdminLoggedIn = profile?.role == "admin"
                            } catch (e: Exception) {
                                isAdminLoggedIn = false
                            }
                        } else {
                            isAdminLoggedIn = false
                        }
                    } else {
                        isAdminLoggedIn = false
                    }
                }

                if (sessionStatus !is SessionStatus.Authenticated && sessionStatus !is SessionStatus.NotAuthenticated) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (!isAdminLoggedIn) {
                    AdminAuthScreen(onLoginSuccess = { isAdminLoggedIn = true })
                } else {
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
                            // 1. Initial check for new orders while app was closed
                            launch {
                                try {
                                    val settings = SharedPreferencesSettings(getSharedPreferences("admin_order_notifs", android.content.Context.MODE_PRIVATE))
                                    val orders = SupabaseConfig.client.from("orders").select {
                                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                                        limit(10)
                                    }.decodeAs<List<Order>>()

                                    for (order in orders) {
                                        val orderId = order.id ?: continue
                                        val isNotified = settings.getBoolean("notified_$orderId", false)
                                        if (!isNotified) {
                                            notificationHelper.showNotification(
                                                "Novo Pedido Recebido!",
                                                "Pedido #${orderId.takeLast(6)} de ${order.customer_name ?: "Cliente"}"
                                            )
                                            settings.putBoolean("notified_$orderId", true)
                                        }
                                    }
                                } catch (e: Exception) {}
                            }

                            // 2. Realtime subscription
                            val channel = SupabaseConfig.client.realtime.channel("admin_orders_channel")
                            val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                                this.table = "orders"
                            }
                            insertFlow.onEach { change ->
                                val customerName = change.record["customer_name"]?.jsonPrimitive?.content ?: "Cliente"
                                val orderIdStr = change.record["id"]?.jsonPrimitive?.content ?: ""
                                val shortOrderId = orderIdStr.takeLast(6)

                                // Mark as notified in settings
                                if (orderIdStr.isNotBlank()) {
                                    val settings = SharedPreferencesSettings(getSharedPreferences("admin_order_notifs", android.content.Context.MODE_PRIVATE))
                                    settings.putBoolean("notified_$orderIdStr", true)
                                }

                                notificationHelper.showNotification(
                                    "Novo Pedido Recebido!",
                                    "Pedido #$shortOrderId de $customerName"
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        SupabaseConfig.client.auth.signOut()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("LOGOUT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFFFF9800),
        secondary = Color(0xFFFDCB58),
        surface = Color(0xFF121212),
        background = Color(0xFF121212),
        onPrimary = Color.White
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun AdminPanel() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProductId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0 && editingProductId == null,
                    onClick = {
                        selectedTab = 0
                        editingProductId = null
                    },
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
                0 -> ProductFormScreen(
                    productId = editingProductId,
                    onSuccess = {
                        editingProductId = null
                        selectedTab = 1
                    }
                )
                1 -> ProductListScreen(
                    onEditProduct = { id ->
                        editingProductId = id
                        selectedTab = 0
                    }
                )
                2 -> OrdersAdminScreen()
                3 -> CategoryAdminScreen()
                4 -> SettingsAdminScreen()
            }
        }
    }
}



