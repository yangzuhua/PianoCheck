package com.example.pianocheck.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 监督过程中的实时状态，由 AudioMonitorService 写入，UI 通过 StateFlow 读取。
 */
data class PracticeState(
    val isRunning: Boolean = false,
    val remainingMs: Long = 0L,
    val level: Float = 0f,          // 当前声音能量 0..1
    val remindedCount: Int = 0,
    val statusText: String = "未开始",
    val reminding: Boolean = false  // 是否正在播放提醒语音
)

object PracticeSessionManager {
    private val _state = MutableStateFlow(PracticeState())
    val state: StateFlow<PracticeState> = _state.asStateFlow()

    fun update(block: (PracticeState) -> PracticeState) {
        _state.value = block(_state.value)
    }

    fun reset() {
        _state.value = PracticeState()
    }
}
