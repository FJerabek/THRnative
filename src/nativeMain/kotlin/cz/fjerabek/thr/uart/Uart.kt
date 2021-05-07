package cz.fjerabek.thr.uart

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.badoo.reaktive.utils.atomic.AtomicReference
import com.badoo.reaktive.utils.atomic.getAndUpdate
import cz.fjerabek.thr.LogUtils.debug
import termios.setupSerial
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.data.uart.*
import glib.*
import kotlinx.cinterop.*
import platform.posix.*

/**
 * Generic uart Exception
 * @param message exception message
 */
open class UartException(message: String) : Exception(message)

/**
 * Error writing to uart
 * @param message exception message
 */
class UartWriteException(message: String) : UartException(message)

/**
 * Error reading message from uart
 * @param message exception message
 */
class UartReadException(message: String) : UartException(message)

@SharedImmutable
val uartQueue = AtomicReference<List<String>>(listOf())


/**
 * Creates uart message from string represetnation
 * @param string string representation of message
 * @return Uart message from string representation
 */
fun UartMessage.Companion.fromString(string: String): UartMessage {
    val params = string.split(";")

    when (params[0].trim()) {
        "\$btn" -> {
            return ButtonMessage(
                params[1].toInt(),
                params[2].trim().toInt() == 1,
                if (params.size == 4) params[3].trim().toLong() else 0L
            )
        }
        "\$ok" -> {
            var cmd = ""
            uartQueue.getAndUpdate {
                it.toMutableList().apply { cmd = removeLast() }
            }
            when (cmd) {
                CMD_SHUTDOWN -> {
                    return ShutdownMessage(true)
                }
                CMD_FW -> {
                    return FWVersionMessage(
                        params[1].trim().toInt(),
                        params[2].trim().toInt(),
                        params[3].trim().toInt()
                    )
                }
                CMD_STATUS -> {
                    return StatusMessage(
                        params[1].trim().toLong(),
                        params[2].trim().toInt(),
                        ECharging.fromValue(params[3])!!,
                        params[4].trim().toInt()
                    )
                }
                CMD_HBT -> {
                    return HbtMessage()
                }
                else -> {
                    throw UnsupportedOperationException("Received invalid message")
                }
            }
        }
        "\$off" -> {
            return ShutdownMessage(true)
        }
        else -> {
            throw UnsupportedOperationException("Received invalid message: $params")
        }
    }
}

/**
 * Object representing uart port
 */
@ExperimentalUnsignedTypes
object Uart {
    private const val UART_FILE = "/dev/ttyS0"
    private val uart: CPointer<GIOChannel>?
    private val fd: Int = open(UART_FILE, O_RDWR or O_NOCTTY or O_SYNC)

    init {
        "Opening UART serial".debug()
        uart = if (fd == -1) {
            throw UartException("Opening failed: ${strerror(errno)?.toKString()}")
            null
        } else {
            "Serial opened fd: $fd".debug()
            setupSerial(fd)
            g_io_channel_unix_new(fd)
    //        g_io_channel_set_buffered(uart, 1/*true*/)
        }
    }

    /**
     * Writes string to UART
     * @param message string message to write
     */
    private fun writeString(message: String): Int = memScoped {
        val buffer = message.encodeToByteArray()
        var written = -1
        buffer.usePinned {
            written = write(fd, it.addressOf(0), message.length.convert()).toInt()
        }

        if (written == -1) {
            throw UartWriteException("Writing to closed socket")
        }

        tcflush(fd, TCIFLUSH)
        return written

    }

    /**
     * Starts uart message receiver
     * @return observable which is called when new message is received
     */
    fun startReceiver() = observable<UartMessage> { observable ->
        if (uart == null) {
            throw UartException("UART not initialized cannot start receiver")
        }
        val buffer = ByteArray(255) { 0 }
        memScoped {
            val read = alloc<gsizeVar>()
            val error = allocPointerTo<GError>()
            val message = allocPointerTo<gcharVar>()
            buffer.usePinned {
                while (true) {
                    try {
                        g_io_channel_read_line(uart, message.ptr, read.ptr, null, error.ptr)
                        error.value?.let {
                            throw UartReadException(
                                it.pointed.message?.toKString() ?: "UART read error without message"
                            )
                        }
                        message.value?.let {
                            observable.onNext(UartMessage.fromString(it.toKString()))
                        }
                    } catch (e: Exception) {
                        e.stackTraceToString().error()
                    }
                }
            }
        }
    }.observeOn(ioScheduler)

    /**
     * Sends firmware request message to uart device
     */
    fun requestFirmware() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(UartMessage.CMD_FW) }
        }
        writeString(UartMessage.CMD_FW)
    }

    /**
     * Sends status request message to uart device
     */
    fun requestStatus() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(UartMessage.CMD_STATUS) }
        }
        writeString(UartMessage.CMD_STATUS)
    }

    /**
     * Sends heart beat message to uart device
     */
    fun sendHeartBeat() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(UartMessage.CMD_HBT) }
        }
        writeString(UartMessage.CMD_HBT)
    }

    /**
     * Sends shutdown request to uart device
     */
    fun requestShutdown() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(UartMessage.CMD_SHUTDOWN) }
        }
        writeString(UartMessage.CMD_SHUTDOWN)
    }

    /**
     * Closes uart connection
     */
    fun close() {
        uart?.let {
            g_io_channel_close(uart)
        }
    }
}