package com.example.drawingapp

import android.app.Application
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class DrawingApplication : Application() {

    // ======= FIREBASE UPDATE ========
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

    // we want a scope tied to the life of the application (not the activity!) so that when
    // lifecycle changes happen and the activity is destroyed, our coroutines aren't destroyed with it
    // We want the coroutines to persist as long as the application is running
    val scope = CoroutineScope(SupervisorJob())

    // get a reference to the DrawingDatabase singleton
    val db by lazy { Room.databaseBuilder(
        applicationContext,
        DrawingDatabase::class.java,
        "drawing_database"
    ).build() }

    // create the repository via lazy so the database is created when it is needed (not initially)
    val drawingRepository by lazy { DrawingRepository(scope, applicationContext, db.drawingDao()) }
}