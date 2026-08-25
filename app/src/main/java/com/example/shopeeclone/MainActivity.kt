package com.example.shopeeclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.shopeeclone.navigation.ShopeeNavGraph
import com.example.shopeeclone.ui.theme.ShopeeCloneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShopeeCloneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShopeeNavGraph()
                }
            }
        }
    }
}
