package com.example.drawingapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import android.view.MotionEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class DrawingViewModel(private val repository: DrawingRepository) : ViewModel() {

    // Firebase stuff
    private val _user = mutableStateOf(Firebase.auth.currentUser)
    val user: State<FirebaseUser?> get() = _user
    var loginError = mutableStateOf<String?>(null)


    private var currentDrawingId: Int? = null
    private var currentFilepath: String? = null

    // Canvas and bitmap dimensions
    internal var imageViewWidth: Int = 48
    internal var imageViewHeight: Int = 48

    // Paint properties
    internal var currentColor = Color.RED
    var currentStrokeSize = mutableFloatStateOf(15f)

    private var downX = 0f
    private var downY = 0f

    // States for bitmap and eraser mode
    private val _bitmap = mutableStateOf<Bitmap?>(null)
    val bitmap: State<Bitmap?> = _bitmap
    private val _isErasing = mutableStateOf(false)
    val isErasing: State<Boolean> = _isErasing

    // Exposing repository drawing list
    val allDrawings: LiveData<List<Drawing>> = repository.allDrawings

    // State to hold the fetched Drawing
    private val _selectedDrawing = mutableStateOf<Drawing?>(null)
    val selectedDrawing: State<Drawing?> = _selectedDrawing

    // Paint objects
    private var currentPaint = Paint().apply {
        color = currentColor
        strokeWidth = currentStrokeSize.floatValue
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private lateinit var canvasBitmap: Bitmap
    private lateinit var canvas: Canvas

    fun onSignupClicked(email: String, password: String) {
        if (email == "" || password == "") {
            loginError.value = "Cannot leave field blank"
            return
        }
        Firebase.auth.createUserWithEmailAndPassword(
            email,
            password
        )
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = Firebase.auth.currentUser
                    loginError.value = null
                } else {
                    loginError.value = "Sign up failed, try again"
                }
            }
    }

    fun onLoginClicked(email: String, password: String) {
        if (email == "" || password == "") {
            loginError.value = "Cannot leave field blank"
            return
        }
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = Firebase.auth.currentUser
                    loginError.value = null
                } else {
                    //display errors in email field.  Hacky, but simple for this demo
                    loginError.value = "Login failed, try again"
                }
            }
    }

    fun onLogoutClicked() {
        Firebase.auth.signOut()
        _user.value = null
    }


    fun onImageClicked(id: Int) {
        viewModelScope.launch {
            val drawing = repository.getDrawingById(id)
            _selectedDrawing.value = drawing
            Log.e("Here!", "$drawing")
        }
    }

    fun setImageViewDimensions(width: Int, height: Int) {
        imageViewWidth = width
        imageViewHeight = height
    }

    fun createBitmap() {
        canvasBitmap = Bitmap.createBitmap(imageViewWidth, imageViewHeight, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap)
        _bitmap.value = canvasBitmap
    }

    fun setEraseMode(erase: Boolean) {
        _isErasing.value = erase

        if (_isErasing.value) {
            currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            currentPaint.alpha = 0
        } else {
            currentPaint.xfermode = null
            currentPaint.alpha = 255
            currentPaint.color = currentColor
        }
        currentPaint.strokeWidth = currentStrokeSize.floatValue
    }

    fun changePaintColor(color: Int) {
        currentColor = color
        if (!_isErasing.value) {
            currentPaint.color = color
        }
    }

    fun changePaintSize(size: Float) {
        currentStrokeSize.floatValue = size
        currentPaint.strokeWidth = size
    }

    fun handleTouchEvent(event: MotionEvent) {
        if (_bitmap.value == null) return

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                canvas.drawCircle(downX, downY, currentStrokeSize.floatValue / 15, currentPaint)
                canvasBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)
                canvas = Canvas(canvasBitmap)
                _bitmap.value = canvasBitmap
            }

            MotionEvent.ACTION_MOVE -> {
                canvas.drawLine(downX, downY, x, y, currentPaint)
                downX = x
                downY = y
                canvasBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)
                canvas = Canvas(canvasBitmap)
                _bitmap.value = canvasBitmap
            }

            MotionEvent.ACTION_UP -> {
                canvas.drawLine(downX, downY, x, y, currentPaint)
                canvasBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)
                canvas = Canvas(canvasBitmap)
                _bitmap.value = canvasBitmap
            }
        }
    }

    fun setBitmap(filepath: String) {
        viewModelScope.launch {
            _bitmap.value = repository.loadDrawing(filepath)
        }
    }

    fun loadExistingDrawing(drawingId: Int) {
        viewModelScope.launch {
            val drawing = repository.getDrawingById(drawingId)
            drawing?.let {
                currentDrawingId = it.id
                currentFilepath = it.filepath
                val loadedBitmap = repository.loadDrawing(it.filepath)
                if (loadedBitmap != null) {
                    canvasBitmap = loadedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    canvas = Canvas(canvasBitmap)
                    _bitmap.value = canvasBitmap
                }
            }
        }
    }

    suspend fun loadBitmapFromStorage(filepath: String): ImageBitmap? {
        val bitmap = repository.loadDrawing(filepath)
        return bitmap?.asImageBitmap()
    }

    fun saveBitmap(drawingId: Int) {
        _bitmap.value?.let { bitmap ->
            if (drawingId != -1) {
                currentFilepath?.let { repository.updateDrawing(bitmap, it) }
            } else {
                repository.saveDrawing(bitmap)
            }
        }
    }

    fun shareDrawing(drawingId: Int, bitmap: Bitmap?){
        if (bitmap != null)
            repository.shareDrawing(bitmap) // should we save it (locally) too?
    }

}


// Factory class for the ViewModel
class DrawingViewModelFactory(private val repository: DrawingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DrawingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DrawingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}