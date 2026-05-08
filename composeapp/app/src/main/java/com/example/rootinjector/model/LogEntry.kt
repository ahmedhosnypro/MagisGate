package com.example.rootinjector.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class LogEntry(val message: String) {
    private val time: LocalTime = LocalTime.now()
    val formatted: String
        get() = "[${time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}] $message"
}
