package com.example.comprafacil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.comprafacil.ui.screens.*
import com.example.comprafacil.ui.theme.CompraFacilTheme
import io.github.jan.supabase.gotrue.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompraFacilTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Check for existing session
                val startDestination = remember {
                    if (SupabaseConfig.client.auth.currentUserOrNull() != null) "home" else "auth"
                }

                val items = listOf(
                    Screen.Home,
                    Screen.Cart,
                    Screen.Profile
                )

                // Hide bottom bar on auth, detail, and orders screens
                val showBottomBar = currentDestination?.route in items.map { it.route }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                tonalElevation = 8.dp
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
                                }
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
