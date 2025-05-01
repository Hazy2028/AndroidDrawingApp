package com.example.drawingapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // LaunchedEffect made by ChatGPT
    // LaunchedEffect with a delay to trigger navigation after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds delay
        onSplashComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Splash Screen by MobGang TM 2024", style = MaterialTheme.typography.headlineSmall)
    }
}
