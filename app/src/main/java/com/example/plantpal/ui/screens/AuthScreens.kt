package com.example.plantpal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.plantpal.R
import com.example.plantpal.ui.theme.PlantPalTheme

@Composable
fun LoginScreen(
    authErrorMessage: String?,
    onLoggedIn: (String, String) -> Unit,
    onGoToSignUp: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var localErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AuthScreenScaffold(
        title = "Welcome back",
        subtitle = "Track every plant like a daily wellness habit.",
        backgroundRes = R.drawable.login_bg
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                localErrorMessage = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = plantPalTextFieldColors()
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                localErrorMessage = null
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = plantPalTextFieldColors()
        )

        val messageToShow = localErrorMessage ?: authErrorMessage
        if (messageToShow != null) {
            Text(
                text = messageToShow,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                localErrorMessage = when {
                    email.isBlank() || password.isBlank() -> "Enter your email and password."
                    else -> {
                        onLoggedIn(email.trim(), password)
                        null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Log In")
        }

        TextButton(onClick = onGoToSignUp, modifier = Modifier.align(Alignment.End)) {
            Text("Need an account? Sign up")
        }
    }
}

@Composable
fun SignUpScreen(
    onSignedUp: (String, String, String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AuthScreenScaffold(
        title = "Create your profile",
        subtitle = "Set up your account so your tracking dashboard feels personal from day one!",
        backgroundRes = R.drawable.signin_bg
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = plantPalTextFieldColors()
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = plantPalTextFieldColors()
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = plantPalTextFieldColors()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            label = { Text("Confirm password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = plantPalTextFieldColors()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                errorMessage = when {
                    name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                        "Fill out every field to create your profile."
                    password.length < 6 ->
                        "Use at least 6 characters for the password."
                    password != confirmPassword ->
                        "Passwords do not match."
                    else -> {
                        onSignedUp(name.trim(), email.trim(), password)
                        null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Create Account")
        }

        TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.End)) {
            Text("Already have an account? Log in")
        }
    }
}

@Composable
fun AuthScreenScaffold(
    title: String,
    subtitle: String,
    backgroundRes: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.background),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "PlantPal",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .size(92.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                content()
            }
        }
    }
}

@Composable
private fun plantPalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    PlantPalTheme {
        AuthScreenScaffold(
            title = "Welcome back",
            subtitle = "Track every plant like a daily wellness habit.",
            backgroundRes = R.drawable.login_bg
        ) {
            OutlinedTextField(
                value = "user@example.com",
                onValueChange = { },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = plantPalTextFieldColors()
            )
            OutlinedTextField(
                value = "password123",
                onValueChange = { },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = plantPalTextFieldColors()
            )
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Log In")
            }
        }
    }
}
