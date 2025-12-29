package com.facemash.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val session = withContext(Dispatchers.IO) {
            AuthApi.checkSession()
        }

        // extract friends manually (simple + safe)
        friends =
            """"friends"\s*:\s*\[(.*?)\]""".toRegex()
                .find(session)
                ?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim().replace("\"", "") }
                ?: emptyList()

        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("Chats") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(friends) { friend ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(friend) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/dp/$friend")
                                .addHeader(
                                    "Cookie",
                                    ApiClient.getCookieHeader() ?: ""
                                )
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = friend,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Divider()
                }
            }
        }
    }
}