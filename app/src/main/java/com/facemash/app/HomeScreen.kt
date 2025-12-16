package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    userInfo: String,
    onLogout: () -> Unit
) {

    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Logged in successfully 🎉", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))
        Text(userInfo)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    AuthApi.logout()
                }
                onLogout()
            }
        }) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(message)
    }
}