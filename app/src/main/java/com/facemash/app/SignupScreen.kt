package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignupScreen(onBackToLogin: () -> Unit) {

    var fName by remember { mutableStateOf("") }
    var lName by remember { mutableStateOf("") }
    var uname by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("Male") }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()                 // ✅ keyboard-aware padding
            .verticalScroll(scrollState) // ✅ allow scroll
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Create Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        /* ───── NAME ROW ───── */
        Row {
            OutlinedTextField(
                value = fName,
                onValueChange = { fName = it },
                label = { Text("First name") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = lName,
                onValueChange = { lName = it },
                label = { Text("Last name") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uname,
            onValueChange = { uname = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Email or Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        /* ───── PASSWORD ROW ───── */
        Row {
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = confirmPass,
                onValueChange = { confirmPass = it },
                label = { Text("Confirm") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("DOB (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        /* ───── SEX ROW ───── */
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(
                selected = sex == "Male",
                onClick = { sex = "Male" }
            )
            Text("Male")

            Spacer(modifier = Modifier.width(16.dp))

            RadioButton(
                selected = sex == "Female",
                onClick = { sex = "Female" }
            )
            Text("Female")
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = {

                val f = fName.trim()
                val l = lName.trim()
                val u = uname.trim()
                val c = contact.trim()
                val d = dob.trim()

                when {
                    f.isEmpty() || l.isEmpty() || u.isEmpty()
                            || c.isEmpty() || pass.isEmpty() || d.isEmpty() -> {
                        message = "Please fill all fields"
                        return@Button
                    }

                    pass != confirmPass -> {
                        message = "Passwords do not match"
                        confirmPass = ""
                        return@Button
                    }

                    d.length != 10 -> {
                        message = "Invalid date of birth"
                        return@Button
                    }
                }

                loading = true
                message = "Creating account..."

                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        AuthApi.signup(f, l, u, c, pass, d, sex)
                    }
                    message = result
                    loading = false
                }
            }
        ) {
            Text(if (loading) "Please wait..." else "Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(message)

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Already have an account? Login")
        }

        Spacer(modifier = Modifier.height(24.dp)) // 👈 breathing room for keyboard
    }
}