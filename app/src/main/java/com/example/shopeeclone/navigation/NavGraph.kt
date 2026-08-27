package com.example.shopeeclone.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.shopeeclone.ui.screens.auth.LoginScreen
import com.example.shopeeclone.ui.screens.auth.SignupScreen
import com.example.shopeeclone.ui.screens.home.HomeScreen
import com.example.shopeeclone.ui.screens.product.ProductDetailScreen
import com.example.shopeeclone.ui.screens.cart.CartScreen
import com.example.shopeeclone.ui.screens.checkout.CheckoutScreen
import com.example.shopeeclone.ui.screens.orders.OrderHistoryScreen
import com.example.shopeeclone.ui.screens.profile.ProfileScreen
import com.example.shopeeclone.ui.screens.profile.EditProfileScreen
import com.example.shopeeclone.ui.screens.seller.SellerDashboardScreen
import com.example.shopeeclone.ui.screens.shop.SellerShopScreen
import com.example.shopeeclone.ui.screens.shop.FollowedShopsScreen

@Composable
fun ShopeeNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onProductClick = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
                onGoToCart = { navController.navigate(Screen.Cart.route) },
                onVisitShop = { sellerId, sellerName ->
                    navController.navigate(Screen.SellerShop.createRoute(sellerId, sellerName))
                }
            )
        }

        composable(Screen.Cart.route) {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Screen.Checkout.route) }
            )
        }

        composable(Screen.Checkout.route) {
            CheckoutScreen(
                onOrderPlaced = {
                    navController.navigate(Screen.OrderHistory.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrderHistory.route) {
            OrderHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                },
                onOrderHistoryClick = { navController.navigate(Screen.OrderHistory.route) },
                onSellerDashboardClick = { navController.navigate(Screen.SellerDashboard.route) },
                onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                onFollowedShopsClick = { navController.navigate(Screen.FollowedShops.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SellerDashboard.route) {
            SellerDashboardScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.SellerShop.route,
            arguments = listOf(
                navArgument("sellerId") { type = NavType.StringType },
                navArgument("sellerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            val encodedName = backStackEntry.arguments?.getString("sellerName") ?: ""
            val sellerName = java.net.URLDecoder.decode(encodedName, "UTF-8")
            SellerShopScreen(
                sellerId = sellerId,
                sellerName = sellerName,
                onBack = { navController.popBackStack() },
                onProductClick = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) }
            )
        }

        composable(Screen.FollowedShops.route) {
            FollowedShopsScreen(
                onBack = { navController.popBackStack() },
                onShopClick = { sellerId, sellerName ->
                    navController.navigate(Screen.SellerShop.createRoute(sellerId, sellerName))
                }
            )
        }
    }
}
