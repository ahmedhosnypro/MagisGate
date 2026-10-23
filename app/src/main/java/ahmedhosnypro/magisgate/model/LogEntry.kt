package ahmedhosnypro.magisgate.model

import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

data class LogEntry(val message: String) {
    private val time: LocalTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

    val formatted: String
        get() = "[${time.format(TimeFormat)}] $message"

    companion object {
        private val TimeFormat = LocalTime.Format {
            hour()
            char(':')
            minute()
            char(':')
            second()
        }
    }
}
