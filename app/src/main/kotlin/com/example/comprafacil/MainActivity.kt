package com.example.comprafacil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.comprafacil.data.CartItem
import com.example.comprafacil.data.Product
import com.example.comprafacil.data.SupabaseConfig
import com.example.comprafacil.ui.AuthViewModel
import com.example.comprafacil.ui.screens.*
import com.example.comprafacil.ui.theme.CompraFacilTheme
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompraFacilTheme {
                val authViewModel: AuthViewModel = viewModel()
                val currentUser by authViewModel.currentUser

                if (currentUser == null) {
                    AuthScreen(authViewModel)
                } else {
                    StoreNavigation(authViewModel)
                }
            }
        }
    }
}

@Composable
fun StoreNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onProductClick = { product ->
                    navController.navigate("details/${product.id}")
                },
                onCartClick = { navController.navigate("cart") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable(
            "details/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            var product by remember { mutableStateOf<Product?>(null) }

            LaunchedEffect(productId) {
                scope.launch {
                    product = SupabaseConfig.client.postgrest["products"]
                        .select(Columns.raw("*, images:product_images(*)")) {
                            filter { eq("id", productId!!) }
                        }.decodeSingleOrNull<Product>()
                }
            }

            product?.let {
                ProductDetailsScreen(
                    product = it,
                    onBack = { navController.popBackStack() },
                    onAddToCart = { quantity ->
                        scope.launch {
                            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                            val item = CartItem(
                                user_id = userId,
                                product_id = it.id!!,
                                quantity = quantity
                            )
                            SupabaseConfig.client.postgrest["cart_items"].insert(item)
                            navController.navigate("cart")
                        }
                    },
                    onBuyNow = { quantity ->
                        scope.launch {
                            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                            val item = CartItem(
                                user_id = userId,
                                product_id = it.id!!,
                                quantity = quantity
                            )
                            SupabaseConfig.client.postgrest["cart_items"].insert(item)
                            navController.navigate("cart")
                        }
                    }
                )
            }
        }
        composable("cart") {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckout = { items, total ->
                    // For simplicity, we navigate to checkout.
                    // In a real app we'd pass the total.
                    navController.navigate("checkout")
                }
            )
        }
        composable("checkout") {
            // Fetch cart items again or use a Shared ViewModel
            var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
            var total by remember { mutableStateOf(0.0) }

            LaunchedEffect(Unit) {
                scope.launch {
                    val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                    cartItems = SupabaseConfig.client.postgrest["cart_items"]
                        .select(Columns.raw("*, product:products(*, images:product_images(*))")) {
                            filter { eq("user_id", userId) }
                        }.decodeList<CartItem>()
                    total = cartItems.sumOf { (it.product?.price ?: 0.0) * it.quantity }
                }
            }

            CheckoutScreen(
                cartItems = cartItems,
                total = total,
                onBack = { navController.popBackStack() },
                onOrderConfirmed = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
