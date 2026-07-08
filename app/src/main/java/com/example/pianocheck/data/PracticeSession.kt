package com.example.pianocheck.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一次练习会话的记录。
 * status: "GOOD" = 弹得好（全程无提醒） / "REMINDED" = 有提醒·没认真弹
 * playedSeconds: 实际检测到钢琴声的秒数（估算）
 */
@Entity(tableName = "sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,      // epoch millis
    val endTime: Long,        // epoch millis
    val plannedMinutes: Int,
    val playedSeconds: Int,
    val remindedCount: Int,
    val status: String,
    val encouragementShown: Boolean
)
