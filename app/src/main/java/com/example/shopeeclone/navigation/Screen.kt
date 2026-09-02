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
    object Chat : Screen("chat/{buyerId}/{buyerName}/{sellerId}/{sellerName}/{productId}/{productName}/{productPrice}/{productImageUrl}") {
        // Compose Navigation can fail to match routes containing an empty path
        // segment (e.g. a product with no photo yields ""), so blank values are
        // substituted with a placeholder here and converted back to "" on decode.
        private const val EMPTY_PLACEHOLDER = "_empty_"

        fun createRoute(
            buyerId: String,
            buyerName: String,
            sellerId: String,
            sellerName: String,
            productId: String = "",
            productName: String = "",
            productPrice: String = "",
            productImageUrl: String = ""
        ): String {
            fun enc(s: String) = java.net.URLEncoder.encode(s.ifBlank { EMPTY_PLACEHOLDER }, "UTF-8")
            return "chat/${enc(buyerId)}/${enc(buyerName)}/${enc(sellerId)}/${enc(sellerName)}/" +
                "${enc(productId)}/${enc(productName)}/${enc(productPrice)}/${enc(productImageUrl)}"
        }

        fun decode(raw: String): String {
            val value = java.net.URLDecoder.decode(raw, "UTF-8")
            return if (value == EMPTY_PLACEHOLDER) "" else value
        }
    }
    object ChatList : Screen("chat_list")
    object SellerStats : Screen("seller_stats")
    object SellerShipping : Screen("seller_shipping/{productId}/{productName}") {
        fun createRoute(productId: String, productName: String): String {
            val encodedName = java.net.URLEncoder.encode(productName, "UTF-8")
            return "seller_shipping/$productId/$encodedName"
        }
    }
}
