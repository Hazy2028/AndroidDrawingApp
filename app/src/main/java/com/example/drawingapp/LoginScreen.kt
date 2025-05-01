package com.example.drawingapp

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.core.ActivityScope
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage


@Composable
fun LoginScreen(
    onLogin: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as DrawingApplication
    val viewModel: DrawingViewModel = viewModel(
        factory = DrawingViewModelFactory(application.drawingRepository)
    )

    val user by viewModel.user
    val loginError by viewModel.loginError


    Column {
        if (user == null) { //show the login stuff only if the user hasn't logged in yet
            Column {
                //UI for inputting username and password
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                Text("Not logged in")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )

                Row {
                    Button(onClick = {
                        viewModel.onLoginClicked(email, password)
                    }) {
                        Text("Log In")
                    }
                    Button(onClick = {
                        viewModel.onSignupClicked(email, password)
                    }) {
                        Text("Sign Up")
                    }
                }
                Text(text = if(loginError != null) loginError.toString() else "")
            }

        }
        else {
            LaunchedEffect(user) {
                onLogin()
            }
        }
    }
}