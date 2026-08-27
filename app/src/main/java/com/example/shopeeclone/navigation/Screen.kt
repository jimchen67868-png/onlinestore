package com.example.shopeeclone.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object OrderHistory : Screen("orders")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object SellerDashboard : Screen("seller_dashboard")
    object SellerShop : Screen("shop/{sellerId}/{sellerName}") {
        fun createRoute(sellerId: String, sellerName: String): String {
            val encodedName = java.net.URLEncoder.encode(sellerName, "UTF-8")
            return "shop/$sellerId/$encodedName"
        }
    }
    object FollowedShops : Screen("followed_shops")
    object SellerVouchers : Screen("seller_vouchers")
}
