package com.example.comprafacil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.comprafacil.ui.theme.CompraFacilTheme
import com.example.comprafacil.utils.NotificationHelper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.realtime.*
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationHelper = NotificationHelper(this)
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.comprafacil.utils.OrderUpdateWorker>(15, java.util.concurrent.TimeUnit.MINUTES).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("order_tracking", androidx.work.ExistingPeriodicWorkPolicy.KEEP, workRequest)

        setContent {
            CompraFacilTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val scope = rememberCoroutineScope()

                // Check for existing session
                val startDestination = remember {
                    if (SupabaseConfig.client.auth.currentUserOrNull() != null) "home" else "auth"
                }

                // Notification Permission
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Handle result */ }

                val sessionStatus by SupabaseConfig.client.auth.sessionStatus.collectAsState()

                LaunchedEffect(sessionStatus) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Setup Realtime Listener for Order Updates
                    val userId = if (sessionStatus is SessionStatus.Authenticated) {
                        (sessionStatus as SessionStatus.Authenticated).session.user?.id
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
                                }
                            )
                        }
                        composable(
                            "product/{productId}",
                            arguments = listOf(navArgument("productId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId") ?: ""
                            ProductDetailsScreen(
                                productId = productId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                onCheckout = {
                                    navController.navigate("checkout")
                                }
                            )
                        }
                        composable("checkout") {
                            CheckoutScreen(
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
