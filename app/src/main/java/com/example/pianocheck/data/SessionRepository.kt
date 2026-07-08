package com.example.pianocheck.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {
    val allSessions: Flow<List<PracticeSession>> = dao.getAll()
    suspend fun insert(session: PracticeSession) = dao.insert(session)
    suspend fun delete(session: PracticeSession) = dao.delete(session)
    suspend fun clear() = dao.clear()
}
