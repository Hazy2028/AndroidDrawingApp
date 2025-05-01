package com.example.drawingapp

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.concurrent.Volatile

@Database(entities = [Drawing::class, ], version = 1, exportSchema = false)
abstract class DrawingDatabase : RoomDatabase() {
    abstract fun drawingDao(): DrawingDAO

    companion object {
        // the companion object is a singleton; only one instance will ever exist
        @Volatile
        private var INSTANCE: DrawingDatabase? = null

        /**
         * Usage ex: val db = DrawingDatabase.getDatabase(myContext)
         */
        fun getDatabase(context: Context): DrawingDatabase {
            // if the INSTANCE is null, create a new db and return; else return the existing one
            return INSTANCE ?: synchronized(this) {
                // if another thread started this before it could initiate lock, return that created db
                if (INSTANCE != null) return INSTANCE!!
                // else we're the first thread creating it
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DrawingDatabase::class.java,
                    "drawing_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}





@Entity // use (tableName = <something>) to give table custom name; default name matches class name
data class Drawing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filepath: String
)

@Dao
interface DrawingDAO{

    // will turn the drawing object into a db-able entity via its fields. (Currently an int ID and a String filepath.)
    @Insert
    suspend fun addDrawingPath(drawing: Drawing)

    @Query("SELECT * FROM Drawing")
    fun getAllDrawings(): Flow<List<Drawing>>

    @Query("SELECT * FROM Drawing WHERE Drawing.id = :id")
    suspend fun getDrawing(id: Int): Drawing?
}