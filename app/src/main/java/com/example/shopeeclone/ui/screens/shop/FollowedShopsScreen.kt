package com.example.shopeeclone.ui.screens.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Follow
import com.example.shopeeclone.data.repository.FollowRepository
import kotlinx.coroutines.launch

class FollowedShopsViewModel(
    private val followRepository: FollowRepository = FollowRepository()
) : ViewModel() {
    val shops = mutableStateOf<List<Follow>>(emptyList())
    val isLoading = mutableStateOf(true)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            shops.value = followRepository.getFollowedShops()
            isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedShopsScreen(
    onBack: () -> Unit,
    onShopClick: (String, String) -> Unit,
    viewModel: FollowedShopsViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Followed Shops") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (viewModel.shops.value.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("You're not following any shops yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(viewModel.shops.value, key = { it.sellerId }) { follow ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShopClick(follow.sellerId, follow.sellerName) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(follow.sellerName, fontWeight = FontWeight.Medium)
                    }
                    Divider()
                }
            }
        }
    }
}
