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
import androidx.navigation.navDeepLink
import com.example.comprafacil.ui.screens.*
import com.example.comprafacil.ui.theme.CompraFacilTheme
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.window.DialogProperties
import com.example.comprafacil.core.data.AppConfig
import com.example.comprafacil.utils.NotificationHelper
import io.github.jan.supabase.gotrue.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.*
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var navController: androidx.navigation.NavHostController? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController?.handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseConfig.initialize(this)

        val notificationHelper = NotificationHelper(this)
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.comprafacil.utils.OrderUpdateWorker>(15, java.util.concurrent.TimeUnit.MINUTES).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("order_tracking", androidx.work.ExistingPeriodicWorkPolicy.KEEP, workRequest)

        setContent {
            CompraFacilTheme {
                val currentNavController = rememberNavController()
                navController = currentNavController
                val navBackStackEntry by currentNavController.currentBackStackEntryAsState()
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
                        val channel = SupabaseConfig.client.realtime.channel("orders_channel_$userId")
                        val changeFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                            table = "orders"
                        }

                        changeFlow.onEach { change ->
                            val newStatus = change.record["status"]?.jsonPrimitive?.content
                            val orderId = change.record["id"]?.jsonPrimitive?.content?.takeLast(6)
                            val orderUserId = change.record["user_id"]?.jsonPrimitive?.content

                            if (orderUserId == userId) {
                                notificationHelper.showNotification(
                                    "Atualização no Pedido",
                                    "O status do seu pedido #$orderId mudou para: $newStatus"
                                )
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
                                        icon = { Icon(screen.icon, contentDescription = null) },
                                        label = { Text(screen.label) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            currentNavController.navigate(screen.route) {
                                                popUpTo(currentNavController.graph.findStartDestination().id) {
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
                        currentNavController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("auth") {
                            AuthScreen {
                                currentNavController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        }
                        composable("home") {
                            HomeScreen(
                                onProductClick = { productId ->
                                    currentNavController.navigate("product/$productId")
                                }
                            )
                        }
                        composable(
                            "product/{productId}",
                            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://comprafacil.ct.ws/#/product/{productId}" },
                                navDeepLink { uriPattern = "https://comprafacil.ct.ws/product/{productId}" },
                                navDeepLink { uriPattern = "http://comprafacil.ct.ws/#/product/{productId}" },
                                navDeepLink { uriPattern = "http://comprafacil.ct.ws/product/{productId}" }
                            )
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId") ?: ""
                            ProductDetailsScreen(
                                productId = productId,
                                onBack = { currentNavController.popBackStack() }
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                onCheckout = {
                                    currentNavController.navigate("checkout")
                                }
                            )
                        }
                        composable("checkout") {
                            CheckoutScreen(
                                onBack = { currentNavController.popBackStack() },
                                onOrderFinished = {
                                    currentNavController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                onLogout = {
                                    currentNavController.navigate("auth") {
                                        popUpTo(currentNavController.graph.startDestinationId) { inclusive = true }
                                    }
                                },
                                onOrdersClick = {
                                    currentNavController.navigate("orders")
                                },
                                onAddressesClick = {
                                    currentNavController.navigate("addresses")
                                }
                            )
                        }
                        composable("addresses") {
                            AddressScreen(
                                onBack = { currentNavController.popBackStack() }
                            )
                        }
                        composable("orders") {
                            OrdersScreen(
                                onBack = { currentNavController.popBackStack() }
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
