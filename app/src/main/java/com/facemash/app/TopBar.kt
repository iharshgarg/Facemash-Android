package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext

@Composable
fun TopBar(
    currentUserName: String,
    currentUserFirstName: String,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 🔵 Facemash (Home)
        TextButton(
            onClick = onHome,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                "Facemash",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 👤 Profile (DP + First Name)
        TextButton(onClick = onProfile) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("${ApiClient.BASE_URL}/dp/$currentUserName")
                        .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                        .allowHardware(false)
                        .build(),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    currentUserFirstName,
                    maxLines = 1
                )
            }
        }

        // 🏠 Home
        TextButton(onClick = onHome) {
            Text("Home", maxLines = 1)
        }

        // 🚪 Logout
        TextButton(onClick = onLogout) {
            Text("Logout", maxLines = 1)
        }
    }
}