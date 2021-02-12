package cz.fjerabek.thr

import com.badoo.reaktive.scheduler.singleScheduler
import com.badoo.reaktive.scheduler.submit
import kotlinx.cinterop.*
import platform.posix.*

object LogUtils {
    private const val ANSI_ESCAPE = "\u001b["
    private const val ANSI_RESET = "\u001b[0m"

    enum class ANSIColor(val ansi: Int) {
        RED(31),
        GREEN(32),
        WHITE(37),
        YELLOW(33)
    }

    enum class LogLevel(private val color: ANSIColor? = null) {
        DEBUG(ANSIColor.WHITE),
        INFO(ANSIColor.GREEN),
        WARN(ANSIColor.YELLOW),
        ERROR(ANSIColor.RED);

        override fun toString(): String {
            return this.color?.let { color(it) { super.toString() } } ?: super.toString()
        }
    }

    fun debug(message: () -> Any) {
        logAsync(LogLevel.DEBUG, message().toString())
    }

    fun info(message: () -> Any) {
        logAsync(LogLevel.INFO, message().toString())
    }

    fun warn(message: () -> Any) {
        logAsync(LogLevel.WARN, message().toString())
    }

    fun error(message: () -> Any) {
        log(LogLevel.ERROR, message().toString())
    }

    fun String.info() {
        info { this }
    }

    fun String.debug() {
        debug { this }
    }

    fun String.error() {
        error { this }
    }

    fun String.warn() {
        warn { this }
    }

    private fun logAsync(level: LogLevel, message: String) {
//        singleScheduler.submit {
            log(level, message)
//        }
    }

    private fun log(level: LogLevel, message: String) {
        val timeString = ByteArray(9)
        val indentedMessage = message.replace("\n", "\n\t\t\t")
        memScoped {
            val time: time_tVar = alloc()
            time(time.ptr)
            timeString.usePinned {
                strftime(it.addressOf(0), 9, "%H:%M:%S", localtime(time.ptr))
            }
        }
        println("${timeString.toKString()}\t[$level]\t${indentedMessage}")
    }

    private fun color(color: ANSIColor, string: () -> String) = "$ANSI_ESCAPE${color.ansi}m${string()}$ANSI_RESET"
}

