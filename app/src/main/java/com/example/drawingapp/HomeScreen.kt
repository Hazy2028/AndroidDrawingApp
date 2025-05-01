package com.example.drawingapp

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.setValue
import java.io.File

@Composable
fun HomeScreen(
    onNavigateToDraw: (Int) -> Unit,
    onNavToGallery: () -> Unit,
    onLogout: () -> Unit
    ) {
    //TODO configure with firebase
    // Initialize the ViewModel with the factory
    val context = LocalContext.current
    val application = context.applicationContext as DrawingApplication
    val viewModel: DrawingViewModel = viewModel(
        factory = DrawingViewModelFactory(application.drawingRepository)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onNavigateToDraw(-1) }) {
            Text(text = "Create Drawing")
        }

        Button(onClick = { onNavToGallery() }) {
            Text("Shared Gallery")
        }

        Button(onClick = {
            viewModel.onLogoutClicked()
            onLogout()
        }) {
            Text(text = "Logout")
        }

        Spacer(modifier = Modifier.height(8.dp))

        DrawingList(viewModel, onNavigateToDraw)

    }

}

@Composable
fun DrawingList(viewModel: DrawingViewModel, onNavigateToDraw: (Int) -> Unit) {
    val drawings by viewModel.allDrawings.observeAsState(initial = emptyList())
    val reversedDrawings = drawings.reversed()
    val context = LocalContext.current

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        columns = GridCells.Fixed(2)
    ) {
        items(reversedDrawings) { drawing ->
            DrawingItem(viewModel, drawing, context, onNavigateToDraw)
        }
    }
}


@Composable
fun DrawingItem(viewModel: DrawingViewModel, drawing: Drawing, context: Context, onNavigateToDraw: (Int) -> Unit) {

    // Load the bitmap from the file path
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(drawing.filepath){
        bitmap = viewModel.loadBitmapFromStorage(drawing.filepath)
    }

    // Display the image if bitmap is successfully loaded
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Drawing ${drawing.id}",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(2.dp, Color.Black)
                .clickable(onClick = {
                    onNavigateToDraw(drawing.id)
                })
        )
    }

}

/**
 * Loads a bitmap from the given file path and converts it to ImageBitmap.
 * Returns null if the file does not exist or cannot be decoded.
 */
fun loadImageBitmapFromFile(context: Context, filepath: String): ImageBitmap? {
    return try {
        val file = File(context.filesDir, filepath)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
