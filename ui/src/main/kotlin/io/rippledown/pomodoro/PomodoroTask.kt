package io.rippledown.pomodoro

data class PomodoroTask(
    var title: String,
    var description: String,
    var progress: Float,
    var backgroundColor: Int,
    var date: String
)