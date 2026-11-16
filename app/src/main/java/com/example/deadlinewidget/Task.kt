package com.example.deadlinewidget

import java.time.LocalDate

data class Task(val id: Long, val name: String, val dueEpochDay: Long) {
    fun daysLeft(): Long = dueEpochDay - LocalDate.now().toEpochDay()
}

fun daysColorRes(days: Long): Int = when {
    days <= 0 -> R.color.red
    days <= 3 -> R.color.orange
    else -> R.color.green
}
