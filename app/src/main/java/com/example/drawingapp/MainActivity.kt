package com.example.drawingapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.fragment
import androidx.navigation.navArgument
import com.example.drawingapp.databinding.ActivityMainBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


// Once our project is entirely Jetpack Compose, we should switch from AppCompatActivity to
// ComponentActivity. It's a newer lightweight base class optimized for Jetpack Compose. Doesn't
// support fragments though.

class MainActivity : AppCompatActivity(){

    // public so tests can access the vm
    val viewModel: DrawingViewModel by viewModels {
        DrawingViewModelFactory((application as DrawingApplication).drawingRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // for testing purposes, use the local emulator so we don't accidentally rack up thousands of $ in bills
        val useEmulator = false
        val ip = "10.0.2.2"
        if (useEmulator){
            Firebase.auth.useEmulator(ip, 9099) // appropriate ports for each emulation type
            Firebase.storage.useEmulator(ip, 8080)
            Firebase.firestore.useEmulator(ip, 9199)
        }

        // Setting up Jetpack Compose Navigator
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()
                }
            }
        }
}

// The Navigator
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "splashScreen", modifier = modifier) {

        // Starts with the splash screen which will automatically navigate to the HomeScreen composable
        composable("splashScreen") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("loginScreen") {
                        // remove splashscreen from backstack
                        popUpTo("splashScreen") { inclusive = true }
                    }
                }
            )
        }

        composable("loginScreen") {
            LoginScreen(
                onLogin = {
                    navController.navigate("homeScreen")
                }
            )
        }

        composable("homeScreen") {
            HomeScreen(
                onNavigateToDraw = { drawingId ->
                    navController.navigate("drawScreen/$drawingId")
                }, onNavToGallery = { navController.navigate("galleryScreen") },
                onLogout = {
                    navController.navigate("loginScreen")
                }
            )
        }

        composable("galleryScreen") {
            GalleryScreen(navController)
        }

        composable(
            route = "drawScreen/{drawingId}",
            arguments = listOf(navArgument("drawingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val drawingId = backStackEntry.arguments?.getInt("drawingId") ?: -1

            DrawScreen(
                drawingId = drawingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
    }
