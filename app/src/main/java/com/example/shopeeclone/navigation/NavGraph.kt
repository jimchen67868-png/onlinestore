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
import com.example.shopeeclone.ui.screens.seller.SellerVoucherScreen
import com.example.shopeeclone.ui.screens.shop.SellerShopScreen
import com.example.shopeeclone.ui.screens.shop.FollowedShopsScreen
import com.example.shopeeclone.ui.screens.chat.ChatScreen
import com.example.shopeeclone.ui.screens.chat.ChatListScreen
import com.example.shopeeclone.data.model.Product

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
                },
                onChatWithSeller = { buyerId, buyerName, product ->
                    navController.navigate(
                        Screen.Chat.createRoute(
                            buyerId = buyerId,
                            buyerName = buyerName,
                            sellerId = product.sellerId,
                            sellerName = product.sellerName,
                            productId = product.id,
                            productName = product.name,
                            productPrice = (product.discountPrice ?: product.price).toString(),
                            productImageUrl = product.imageUrl
                        )
                    )
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
                onChatListClick = { navController.navigate(Screen.ChatList.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SellerDashboard.route) {
            SellerDashboardScreen(
                onBack = { navController.popBackStack() },
                onManageVouchers = { navController.navigate(Screen.SellerVouchers.route) }
            )
        }

        composable(Screen.SellerVouchers.route) {
            SellerVoucherScreen(onBack = { navController.popBackStack() })
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
                onProductClick = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                onChatWithSeller = { buyerId, buyerName, sId, sName ->
                    navController.navigate(Screen.Chat.createRoute(buyerId, buyerName, sId, sName))
                }
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

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("buyerId") { type = NavType.StringType },
                navArgument("buyerName") { type = NavType.StringType },
                navArgument("sellerId") { type = NavType.StringType },
                navArgument("sellerName") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType },
                navArgument("productPrice") { type = NavType.StringType },
                navArgument("productImageUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            fun dec(key: String) = java.net.URLDecoder.decode(backStackEntry.arguments?.getString(key) ?: "", "UTF-8")
            ChatScreen(
                buyerId = dec("buyerId"),
                buyerName = dec("buyerName"),
                sellerId = dec("sellerId"),
                sellerName = dec("sellerName"),
                productId = dec("productId"),
                productName = dec("productName"),
                productPrice = dec("productPrice"),
                productImageUrl = dec("productImageUrl"),
                onBack = { navController.popBackStack() },
                onProductClick = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                onOrderClick = { navController.navigate(Screen.OrderHistory.route) }
            )
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { buyerId, buyerName, sellerId, sellerName ->
                    navController.navigate(Screen.Chat.createRoute(buyerId, buyerName, sellerId, sellerName))
                }
            )
        }
    }
}
