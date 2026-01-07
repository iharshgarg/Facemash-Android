package com.facemash.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.ContentScale

private val FacebookBlue = androidx.compose.ui.graphics.Color(0xFF3B5998)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    var friends by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        val session = withContext(Dispatchers.IO) {
            AuthApi.checkSession()
        }

        // extract friends safely
        friends =
            """"friends"\s*:\s*\[(.*?)\]""".toRegex()
                .find(session)
                ?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim().replace("\"", "") }
                ?.filter { it.isNotBlank() }
                ?.reversed()
                ?: emptyList()

        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {

        /* ───── TOP BAR ───── */
        Surface(
            color = FacebookBlue,
            shadowElevation = 6.dp
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Chats",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FacebookBlue
                )
            )
        }

        /* ───── CONTENT ───── */
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            friends.isEmpty() -> {
                // ✅ EMPTY STATE
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add friends to start chatting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
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
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
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
}