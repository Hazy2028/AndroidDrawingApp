package com.example.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.asLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class DrawingRepository(
    private val scope: CoroutineScope,
    private val context: Context,
    private val dao: DrawingDAO
) {

    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val storageRef: StorageReference by lazy { storage.reference.child("drawings")}

    val allDrawings = dao.getAllDrawings().asLiveData()

    /**
     * Calls helper to save bitmap to file as PNG. Inserts the drawing into the db.
     */
    fun saveDrawing(bitmap: Bitmap){
        scope.launch {
            try {
                val storagePath = "${Firebase.auth.currentUser!!.uid}/${System.currentTimeMillis()}.png"
                val imageRef = storageRef.child(storagePath)

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val data = baos.toByteArray()

                imageRef.putBytes(data).await()

                val drawing = Drawing(filepath = storagePath)
                dao.addDrawingPath(drawing)

            } catch(e: Exception){
                e.printStackTrace()
            }

        }
    }

    /**
     * Send a filepath from the db to this function to load the corresponding bitmap.
     * Returns null if anything goes wrong (can't be decoded, not a bitmap, filepath invalid, etc.).
     */
    suspend fun loadDrawing(filepath: String): Bitmap? { //TODO refactor to use return suspendCoroutine
        return try {
            val imageRef = storageRef.child(filepath)
            val MAX_SIZE: Long = 5*1024*1024
            val bytes = imageRef.getBytes(MAX_SIZE).await()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    suspend fun getDrawingById(id: Int): Drawing? {
        return dao.getDrawing(id)
    }


    fun updateDrawing(bitmap: Bitmap, filepath:String){
        scope.launch {
            try{
                val imageRef = storageRef.child(filepath)

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val data = baos.toByteArray()

                imageRef.putBytes(data).await()
            }
           catch(e: Exception){
               e.printStackTrace()
           }
        }
    }

    fun shareDrawing(bitmap: Bitmap) {
        val uid = Firebase.auth.currentUser!!.uid
        val pubStorageRef: StorageReference = storage.reference.child("shared")
            .child("$uid/${System.currentTimeMillis()}.png")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        pubStorageRef.putBytes(data)
            .addOnSuccessListener {
                // indicate success
                Toast.makeText(context, "Drawing shared! Check the gallery!",
                    Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                // indicate failure
            }
    }




}