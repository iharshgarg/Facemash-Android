package com.facemash.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun AppStatusScreen(
    message: String
) {
    // trigger animation once
    var visible by remember { mutableStateOf(false) }

    // subtle fade values
    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "logoFade"
    )

    val bottomAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 200),
        label = "bottomFade"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Surface {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            /* 🖼️ LOGO + TITLE — CENTER */
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(logoAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Facemash Logo",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Facemash",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Indian Social Network",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            /* 🔄 LOADER + STATUS — LOWER PART */
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .alpha(bottomAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}