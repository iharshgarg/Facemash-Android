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
fun SignupScreen(onBackToLogin: () -> Unit) {

    var fName by remember { mutableStateOf("") }
    var lName by remember { mutableStateOf("") }
    var uname by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("Male") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Create Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(fName, { fName = it }, label = { Text("First name") })
        OutlinedTextField(lName, { lName = it }, label = { Text("Last name") })
        OutlinedTextField(uname, { uname = it }, label = { Text("Username") })
        OutlinedTextField(contact, { contact = it }, label = { Text("Email or Phone") })
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") })
        OutlinedTextField(dob, { dob = it }, label = { Text("DOB (YYYY-MM-DD)") })

        Spacer(modifier = Modifier.height(8.dp))

        Row {
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

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            enabled = !loading,
            onClick = {
                loading = true
                message = "Creating account..."

                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        AuthApi.signup(fName, lName, uname, contact, pass, dob, sex)
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

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Already have an account? Login")
        }
    }
}