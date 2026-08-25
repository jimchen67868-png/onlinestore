package com.example.shopeeclone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ShopeeColorScheme = lightColorScheme(
    primary = ShopeeOrange,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = ShopeeOrangeDark,
    background = Background,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ShopeeCloneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShopeeColorScheme,
        content = content
    )
}
