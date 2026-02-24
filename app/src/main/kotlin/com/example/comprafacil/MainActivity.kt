package com.example.comprafacil
import com.example.comprafacil.core.SupabaseConfig

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.comprafacil.ui.screens.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comprafacil.ui.theme.CompraFacilTheme
import com.example.comprafacil.ui.viewmodel.CartViewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.window.DialogProperties
import com.example.comprafacil.core.data.AppConfig
import com.example.comprafacil.utils.NotificationHelper
import com.example.comprafacil.core.data.Order
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.gotrue.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.*
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseConfig.initialize(this)

        val notificationHelper = NotificationHelper(this)
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.comprafacil.utils.OrderUpdateWorker>(15, java.util.concurrent.TimeUnit.MINUTES).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("order_tracking", androidx.work.ExistingPeriodicWorkPolicy.KEEP, workRequest)

        setContent {
            CompraFacilTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val scope = rememberCoroutineScope()

                val sessionStatus by SupabaseConfig.client.auth.sessionStatus.collectAsState()

                var showUpdateDialog by remember { mutableStateOf(false) }
                var downloadUrl by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    try {
                        val configs = SupabaseConfig.client.from("app_config").select().decodeAs<List<AppConfig>>()
                        var minVersion = "1.0"
                        configs.forEach { config ->
                            when (config.key) {
                                "min_version" -> minVersion = config.value.jsonPrimitive.content
                                "download_url" -> downloadUrl = config.value.jsonPrimitive.content
                            }
                        }
                        val currentVersion = "1.0"
                        if (minVersion > currentVersion) {
                            showUpdateDialog = true
                        }
                    } catch (e: Exception) {}
                }

                if (showUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Atualização Necessária") },
                        text = { Text("Uma nova versão do app está disponível. Por favor, atualize para continuar.") },
                        confirmButton = {
                            Button(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                startActivity(intent)
                            }) {
                                Text("ATUALIZAR AGORA")
                            }
                        },
                        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                    )
                }

                if (sessionStatus !is SessionStatus.Authenticated && sessionStatus !is SessionStatus.NotAuthenticated) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    return@CompraFacilTheme
                }

                val startDestination = remember {
                    if (sessionStatus is SessionStatus.Authenticated) "home" else "auth"
                }

                // Notification Permission
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Handle result */ }

                LaunchedEffect(sessionStatus) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Setup Realtime Listener for Order Updates
                    val status = sessionStatus
                    val userId = if (status is SessionStatus.Authenticated) {
                        status.session.user?.id
                    } else null

                    if (userId != null) {
                        // 1. Initial Check: Fetch current status of active orders to catch updates while app was closed
                        launch {
                            try {
                                val settings = SharedPreferencesSettings(getSharedPreferences("order_notifications", android.content.Context.MODE_PRIVATE))
                                val orders = SupabaseConfig.client.from("orders").select {
                                    filter {
                                        eq("user_id", userId)
                                        neq("status", "concluído")
                                        neq("status", "cancelado")
                                    }
                                }.decodeAs<List<Order>>()

                                for (order in orders) {
                                    val orderId = order.id ?: continue
                                    val currentStatus = order.status ?: "pendente"
                                    val lastKnownStatus = settings.getString("order_$orderId", "pendente")

                                    if (currentStatus != lastKnownStatus) {
                                        notificationHelper.showNotification(
                                            "Atualização no Pedido #${orderId.takeLast(6)}",
                                            "O status mudou para: ${currentStatus.uppercase()}"
                                        )
                                        settings.putString("order_$orderId", currentStatus)
                                    }
                                }
                            } catch (e: Exception) {
                                // Silent fail for initial check
                            }
                        }

                        // 2. Realtime Listener: Subscribe to updates
                        val channel = SupabaseConfig.client.realtime.channel("orders_channel_$userId")
                        val changeFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                            table = "orders"
                        }

                        changeFlow.onEach { change ->
                            val orderUserId = change.record["user_id"]?.jsonPrimitive?.content

                            // Client-side filtering (Reliable now with REPLICA IDENTITY FULL)
                            if (orderUserId == userId) {
                                val newStatus = change.record["status"]?.jsonPrimitive?.content
                                val orderId = change.record["id"]?.jsonPrimitive?.content?.takeLast(6)

                                // Save status to settings to avoid double notifications with worker/initial check
                                val fullOrderId = change.record["id"]?.jsonPrimitive?.content
                                if (fullOrderId != null && newStatus != null) {
                                    val settings = SharedPreferencesSettings(getSharedPreferences("order_notifications", android.content.Context.MODE_PRIVATE))
                                    val lastKnownStatus = settings.getString("order_$fullOrderId", "pendente")

                                    if (newStatus != lastKnownStatus) {
                                        notificationHelper.showNotification(
                                            "Atualização no Pedido #$orderId",
                                            "O status mudou para: ${newStatus.uppercase()}"
                                        )
                                        settings.putString("order_$fullOrderId", newStatus)
                                    }
                                }
                            }
                        }.launchIn(this)

                        channel.subscribe()
                    }
                }

                val items = listOf(
                    Screen.Home,
                    Screen.Cart,
                    Screen.Profile
                )

                val showBottomBar = currentDestination?.route in items.map { it.route }
                val cartViewModel: CartViewModel = viewModel()
                val cartCount by cartViewModel.cartCount.collectAsState()

                Scaffold(
                    // Set Scaffold background to match the theme background exactly
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            // Fix "floating line": Set tonalElevation to 0 and use a clear surface color
                            // We don't override windowInsets here to allow the NavigationBar to respect system bars (especially on Android 15+)
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.primary,
                                tonalElevation = 0.dp
                            ) {
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            if (screen == Screen.Cart) {
                                                BadgedBox(badge = {
                                                    if (cartCount > 0) {
                                                        Badge { Text(cartCount.toString()) }
                                                    }
                                                }) {
                                                    Icon(screen.icon, contentDescription = null)
                                                }
                                            } else {
                                                Icon(screen.icon, contentDescription = null)
                                            }
                                        },
                                        label = { Text(screen.label) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                                            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("auth") {
                            AuthScreen {
                                navController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        }
                        composable("home") {
                            HomeScreen(
                                onProductClick = { productId ->
                                    navController.navigate("product/$productId")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                }
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                onProductClick = { productId ->
                                    navController.navigate("product/$productId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "product/{productId}",
                            arguments = listOf(navArgument("productId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId") ?: ""
                            ProductDetailsScreen(
                                productId = productId,
                                onBack = { navController.popBackStack() },
                                onBuyNow = { pid, qty, vars ->
                                    val varsJson = if (vars.isNotEmpty()) {
                                        kotlinx.serialization.json.Json.encodeToString(vars)
                                    } else null

                                    val route = buildString {
                                        append("checkout?productId=$pid")
                                        append("&quantity=$qty")
                                        if (varsJson != null) {
                                            append("&variations=${Uri.encode(varsJson)}")
                                        }
                                    }
                                    navController.navigate(route)
                                }
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                onCheckout = {
                                    navController.navigate("checkout")
                                }
                            )
                        }
                        composable(
                            "checkout?productId={productId}&quantity={quantity}&variations={variations}",
                            arguments = listOf(
                                navArgument("productId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("quantity") { type = NavType.IntType; defaultValue = 1 },
                                navArgument("variations") { type = NavType.StringType; nullable = true; defaultValue = null }
                            )
                        ) { backStackEntry ->
                            CheckoutScreen(
                                productId = backStackEntry.arguments?.getString("productId"),
                                quantity = backStackEntry.arguments?.getInt("quantity") ?: 1,
                                variationsJson = backStackEntry.arguments?.getString("variations"),
                                onBack = { navController.popBackStack() },
                                onOrderFinished = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                onLogout = {
                                    navController.navigate("auth") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                },
                                onOrdersClick = {
                                    navController.navigate("orders")
                                },
                                onAddressesClick = {
                                    navController.navigate("addresses")
                                }
                            )
                        }
                        composable("addresses") {
                            AddressScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("orders") {
                            OrdersScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object Cart : Screen("cart", "Carrinho", Icons.Default.ShoppingCart)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
}
