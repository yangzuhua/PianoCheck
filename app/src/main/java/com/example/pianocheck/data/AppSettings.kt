package com.example.pianocheck.data

import java.io.Serializable

/**
 * 家长自定义设置。
 * - practiceMinutes：练习总时长（分钟），倒计时用
 * - silenceSeconds：连续多少秒听不到钢琴声就触发提醒
 * - sensitivity：麦克风灵敏度阈值（0.005~0.1，越小越灵敏）
 * - remindText：静音提醒文案（自定义输入）
 * - encourageText：顺利完成且无提醒时的鼓励文案（自定义输入）
 */
data class AppSettings(
    val practiceMinutes: Int = 30,
    val silenceSeconds: Int = 30,
    val sensitivity: Float = 0.02f,
    val remindText: String = "宝贝，继续弹琴哦，不要停~",
    val encourageText: String = "今天弹得真棒，继续保持！"
) : Serializable
