package cz.fjerabek.thr

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.AtomicReference

@ExperimentalUnsignedTypes
object LogUtils {
    enum class LogLevel(private val color: ANSIColor) {
        DEBUG(ANSIColor.WHITE),
        INFO(ANSIColor.GREEN),
        WARN(ANSIColor.YELLOW),
        ERROR(ANSIColor.RED);

        override fun toString(): String {
            return super.toString().color(color)
        }
    }
     val logLevel = AtomicReference(LogLevel.INFO)

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
        if(logLevel.value > level) return
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

}

