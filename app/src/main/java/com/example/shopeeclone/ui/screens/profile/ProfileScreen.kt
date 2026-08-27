package com.example.shopeeclone.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOrderHistoryClick: () -> Unit,
    onSellerDashboardClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onFollowedShopsClick: () -> Unit,
    onChatListClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ListItem(
                headlineContent = { Text("Edit Profile") },
                modifier = Modifier.clickableRow(onEditProfileClick)
            )
            Divider()
            ListItem(
                headlineContent = { Text("My Orders") },
                modifier = Modifier.clickableRow(onOrderHistoryClick)
            )
            Divider()
            ListItem(
                headlineContent = { Text("My Chats") },
                modifier = Modifier.clickableRow(onChatListClick)
            )
            Divider()
            ListItem(
                headlineContent = { Text("Followed Shops") },
                modifier = Modifier.clickableRow(onFollowedShopsClick)
            )
            Divider()
            ListItem(
                headlineContent = { Text("Seller Dashboard") },
                modifier = Modifier.clickableRow(onSellerDashboardClick)
            )
            Divider()
            Spacer(Modifier.height(24.dp))
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out")
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
