package com.example.drawingapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// code written in part with the help of Microsoft Copilot
@Composable
fun GalleryScreen(navController: NavController) {
    var imagePaths by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // get the image paths and the user names (which are stored in the path)
    LaunchedEffect(Unit) {
        imagePaths = fetchSharedImagesPathAndUid()
    }

    if (imagePaths.isEmpty()) {
      Text("Oh no! Looks like no one has shared any images :'(\n\nHow about you share one!")
    } else {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(imagePaths) { (path, uid) ->
                val bitmap = remember { mutableStateOf<ImageBitmap?>(null) }

                // get the current image (looping via items call above)
                LaunchedEffect(path) {
                    try {
                        val storageRef = FirebaseStorage.getInstance().reference
                        val downloadedBitmap = downloadImage(storageRef, path)
                        downloadedBitmap?.let {
                            bitmap.value = it.asImageBitmap()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    bitmap.value?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "drawing",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(screenHeight * .85f)
                                .border(2.dp, Color.Black)
                        )
                    }

                    Text("by $uid")
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

suspend fun downloadImage(ref: StorageReference, path: String): Bitmap? {
    val fileRef = ref.child(path)
    return suspendCoroutine { cont ->
        fileRef.getBytes(10*1024*1024).addOnSuccessListener { bytes ->
            cont.resume(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        }.addOnFailureListener { e ->
            e.printStackTrace()
            cont.resume(null)
        }
    }
}

// not good to have in a Composable but oh well. Belongs in repository.
suspend fun fetchSharedImagesPathAndUid(): List<Pair<String, String>> {
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference.child("shared")
    val imagePaths = mutableListOf<Triple<String, String, Long>>()
    // suspend coroutine? Then we can't await()
    try {
        val result = storageRef.listAll().await()
        for (folder in result.prefixes) {
            val uid = folder.name
            val usersDrawings = folder.listAll().await()
            for (fileRef in usersDrawings.items) {
                val metadata = fileRef.metadata.await()
                imagePaths.add(Triple(fileRef.path, uid, metadata.creationTimeMillis))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    // sort the images by their creation time and convert the triples into pairs
    imagePaths.sortByDescending { it.third }
    return imagePaths.map { Pair(it.first, it.second) }
}
