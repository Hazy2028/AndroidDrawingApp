package com.example.drawingapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.drawingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnTouchListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mImageView: ImageView
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private lateinit var highlightPaint: Paint  // Used for debugging.

    // Default Variables: On start we are going ot have the
    // eraser toggled off, default color is red.
    private var isErasing = false           // Toggle for erase mode
    private var defaultColor = Color.RED    // Default drawing color
    private var highlightRadius = 0f        // Radius for highlighting the point
                                            // Used for debugging.
                                            // Make it greater than 0 for debugging

    // Initialize our variables to draw on the canvas.

    //Starting (x,y) point.
    private var downX = 0f                  //
    private var downY = 0f

    // Ending (x,y) point.
    private var upX = 0f
    private var upY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mImageView = binding.imageView

        // Wait until the ImageView is laid out to get its dimensions
        mImageView.post {
            val imageViewWidth = mImageView.width
            val imageViewHeight = mImageView.height

            // Create bitmap with the same dimensions as the ImageView
            bitmap = Bitmap.createBitmap(imageViewWidth, imageViewHeight, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap)
            paint = Paint().apply {
                color = defaultColor // Default drawing color
                strokeWidth = 10f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            // Paint for highlighting the point when clicked
            highlightPaint = Paint().apply {
                color = Color.YELLOW // Highlight color
                strokeWidth = 5f
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            mImageView.setImageBitmap(bitmap)
            mImageView.setOnTouchListener(this)
        }

        // Color picker button listener
        binding.btnColorPicker.setOnClickListener { openColorPickerDialog() }

        // Erase button listener
        binding.btnErase.setOnClickListener { toggleEraseMode() }
    }

    // Open custom color picker dialog with predefined colors
    private fun openColorPickerDialog() {
        val colors = arrayOf("Red", "Green", "Blue", "Yellow", "Black", "Magenta")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick a Color")
        builder.setItems(colors) { _, which ->
            when (colors[which]) {
                "Red" -> changePaintColor(Color.RED)
                "Green" -> changePaintColor(Color.GREEN)
                "Blue" -> changePaintColor(Color.BLUE)
                "Yellow" -> changePaintColor(Color.YELLOW)
                "Black" -> changePaintColor(Color.BLACK)
                "Magenta" -> changePaintColor(Color.MAGENTA)
            }
        }
        builder.show()
    }

    // Change paint color only if not in erase mode
    private fun changePaintColor(color: Int) {
        if (!isErasing) {
            paint.color = color
            defaultColor = color // Update default color
        }
    }

    // TODO: MAKE THE STROKES BIGGER AND SMALLER.
    // TODO: MAKE THE PICTURE DARK WHEN CLICKED AND LIGHT WHEN NOT TOGGLED.
    // Toggle between erase mode and drawing mode
    private fun toggleEraseMode() {
        isErasing = !isErasing
        if (isErasing) {
            // Set eraser mode
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            paint.strokeWidth = 50F // Eraser stroke width
        } else {
            // Set to drawing mode
            paint.xfermode = null
            paint.strokeWidth = 10F // Default stroke width
            paint.color = defaultColor // Restore last selected color
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.let {
            // Get touch coordinates relative to the ImageView
            val x = it.x
            val y = it.y

            // Ensure the touch point is within the bitmap bounds
            if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                when (it.action) {

                    // Record when we first touch the screen.
                    MotionEvent.ACTION_DOWN -> {
                        downX = x
                        downY = y

                        //For debugging the start value.
                        canvas.drawCircle(x, y, highlightRadius, highlightPaint)
                        mImageView.invalidate() // Redraw the imageview.
                    }

                    // We record the values as we move.
                    MotionEvent.ACTION_MOVE -> {
                        upX = x
                        upY = y
                        canvas.drawLine(downX, downY, upX, upY, paint)
                        mImageView.invalidate() // / Redraw the imageview.
                        downX = upX
                        downY = upY
                    }

                    // Finalizing the drawing (last value).
                    MotionEvent.ACTION_UP -> {
                        upX = x
                        upY = y
                        canvas.drawLine(downX, downY, upX, upY, paint)
                        mImageView.invalidate() // / Redraw the imageview.
                    }
                }
            }
        }
        return true
    }
}