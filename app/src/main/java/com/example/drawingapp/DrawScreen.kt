package com.example.drawingapp

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource


//Trying to get into real time.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawScreen(
    drawingId: Int,
    onNavigateBack: () -> Unit
) {
    // Initialize the ViewModel with the factory
    val context = LocalContext.current
    val application = context.applicationContext as DrawingApplication
    val viewModel: DrawingViewModel = viewModel(
        factory = DrawingViewModelFactory(application.drawingRepository)
    )

    // Observe bitmap and isErasing from the ViewModel
    val bitmap by viewModel.bitmap
    val isErasing by viewModel.isErasing
    val currentSize by viewModel.currentStrokeSize

    // States to control dialog visibility
    var showColorPicker by remember { mutableStateOf(false) }
    var showSizePicker by remember { mutableStateOf(false) }

    // State to capture the size of the drawing area
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Initialize the bitmap when the canvas size is available
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0 && bitmap == null) {
            viewModel.setImageViewDimensions(canvasSize.width, canvasSize.height)
            if(drawingId != -1){
                viewModel.loadExistingDrawing(drawingId)
            }else{
                viewModel.createBitmap()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.White)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {

            Button(
                onClick = {viewModel.setEraseMode(false)},
                colors = if (isErasing) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(containerColor = ComposeColor.LightGray)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.brush),
                    contentDescription = "Brush Icon",
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = { viewModel.setEraseMode(true) },
                colors = if (isErasing) {
                    ButtonDefaults.buttonColors(containerColor = ComposeColor.LightGray)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.eraser),
                    contentDescription = "Eraser Icon",
                    modifier = Modifier.size(24.dp)
                )
            }
            Button(onClick = { showColorPicker = true }) {
                Text(text = "Color")
            }
            Button(onClick = { showSizePicker = true }) {
                Text(text = "Size")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ComposeColor.LightGray)
                .onSizeChanged { size ->
                    canvasSize = size
                }
                .pointerInteropFilter { motionEvent ->
                    viewModel.handleTouchEvent(motionEvent)
                    true
                },
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Drawing Canvas",
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Text("Loading...", color = ComposeColor.Gray)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(
                onClick = {
                    viewModel.saveBitmap(drawingId)
                    onNavigateBack()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(text = "Save and Back")
            }

            Button(
                onClick = {
                    viewModel.shareDrawing(drawingId, bitmap)
                          },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) { Text("Share") }
        }

        // Color Picker Dialog
        if (showColorPicker) {
            ColorPicker(
                onColorSelected = { color ->
                    viewModel.changePaintColor(color.toArgb())
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }

        // Paint Size Dialog
        if (showSizePicker) {
            SizePicker(
                currentSize,
                onSizeSelected = { size ->
                    viewModel.changePaintSize(size)
                    showSizePicker = false
                },
                onDismiss = { showSizePicker = false }
            )
        }
    }
}

@Composable
fun ColorPicker(
    onColorSelected: (ComposeColor) -> Unit,
    onDismiss: () -> Unit
) {
    // Define a list of color options
    val colors = listOf(
        "Red" to ComposeColor.Red,
        "Green" to ComposeColor.Green,
        "Blue" to ComposeColor.Blue,
        "Yellow" to ComposeColor.Yellow,
        "Black" to ComposeColor.Black,
        "Magenta" to ComposeColor.Magenta
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Pick a Color") },
        text = {
            Column {
                colors.forEach { (name, color) ->
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            onColorSelected(color)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Text(text = name, color = ComposeColor.White)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun SizePicker(
    initialSize: Float,
    onSizeSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSize by remember { mutableFloatStateOf(initialSize) }

    // Define a list of brush sizes
    val sizes = listOf(5f, 10f, 15f, 20f, 25f, 30f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Pick a Pen Size") },
        text = {
            Column {
                Text(text = "Stroke Width: ${selectedSize.toInt()}")
                Slider(
                    value = selectedSize,
                    onValueChange = { selectedSize = it },
                    valueRange = 1f..50f
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSizeSelected(selectedSize) }) {
                Text(text = "Select")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
