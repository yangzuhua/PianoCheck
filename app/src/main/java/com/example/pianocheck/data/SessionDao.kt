package com.example.pianocheck.data

import androidx.room.*

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: PracticeSession): Long

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<PracticeSession>>

    @Delete
    suspend fun delete(session: PracticeSession)

    @Query("DELETE FROM sessions")
    suspend fun clear()
}
