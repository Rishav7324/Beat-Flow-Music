package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: MusicDatabase? = null

    fun getDatabase(context: Context): MusicDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                MusicDatabase::class.java,
                "pulse_music_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
