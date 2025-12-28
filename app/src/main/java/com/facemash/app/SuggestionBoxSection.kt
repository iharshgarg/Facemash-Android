package com.facemash.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SuggestionBoxSection(
    currentUsername: String,
    onUserClick: (String) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            users = withContext(Dispatchers.IO) {
                AuthApi.fetchSuggestionUsers()
                    .reversed()                     // 🔄 newest first
                    .filter { it.uname != currentUsername } // 🙈 hide self
            }
        }
    }

    if (users.isNotEmpty()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                items(users) { user ->

                    Column(
                        modifier = Modifier
                            .width(80.dp)
                            .clickable {
                                onUserClick(user.uname)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // 👤 DP (CROP, NO EMPTY SPACE)
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/dp/${user.uname}")
                                .addHeader(
                                    "Cookie",
                                    ApiClient.getCookieHeader() ?: ""
                                )
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${user.fName} ${user.lName}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}