package com.example.pianocheck.data

import android.content.Context
import androidx.room.*

@Database(entities = [PracticeSession::class], version = 1, exportSchema = false)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: SessionDatabase? = null

        fun get(context: Context): SessionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "piano_sessions.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
