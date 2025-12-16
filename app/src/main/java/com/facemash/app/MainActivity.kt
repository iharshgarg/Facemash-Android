package com.facemash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(applicationContext)

        setContent {

            var showSignup by remember { mutableStateOf(false) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var userInfo by remember { mutableStateOf("") }
            var checkingSession by remember { mutableStateOf(true) }

            val scope = rememberCoroutineScope()

            // Auto-login check on app start
            LaunchedEffect(Unit) {
                val sessionResult = withContext(Dispatchers.IO) {
                    AuthApi.checkSession()
                }

                if (sessionResult.contains("uname")) {
                    isLoggedIn = true
                    userInfo = sessionResult
                }
                checkingSession = false
            }

            when {
                checkingSession -> {
                    Text("Checking session...")
                }
                isLoggedIn -> {
                    FeedScreen {
                        ApiClient.clearCookies(applicationContext)
                        isLoggedIn = false
                        userInfo = ""
                    }
                }
                else -> {
                    if (showSignup) {
                        SignupScreen {
                            showSignup = false
                        }
                    } else {
                        LoginScreen(
                            onLoginSuccess = { sessionData ->
                                isLoggedIn = true
                                userInfo = sessionData
                            },
                            onSignup = {
                                showSignup = true
                            }
                        )
                    }
                }
            }
        }
    }
}