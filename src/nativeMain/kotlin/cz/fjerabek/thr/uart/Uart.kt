package cz.fjerabek.thr.uart

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.badoo.reaktive.utils.atomic.AtomicReference
import com.badoo.reaktive.utils.atomic.getAndUpdate
import termios.setupSerial
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.data.uart.*
import glib.*
import kotlinx.cinterop.*
import platform.posix.*

open class UartException(message: String) : Exception(message)
class UartWriteException(message: String) : UartException(message)
class UartReadException(message: String) : UartException(message)

@SharedImmutable
val uartQueue = AtomicReference<List<String>>(listOf())


    fun UartMessage.Companion.fromString(string: String): UartMessage {
        val params = string.split(";")

        when(params[0].trim()) {
            "\$btn" -> {
                return ButtonMessage(
                    params[1].toInt(),
                    params[2].trim().toInt() == 1,
                    if (params.size == 4) params[3].trim().toLong() else 0L
                )
            }
            "\$ok" -> {
                var cmd: String = ""
                uartQueue.getAndUpdate {
                    it.toMutableList().apply { cmd = removeLast() }
                }
                when (cmd) {
                    CMD_SHUTDOWN -> {
                        return ShutdownMessage(true);
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
                    CMD_HBT -> { return HbtMessage()
                    }
                    else -> {
                        throw UnsupportedOperationException("Received invalid message")
                    }
                }
            }
            else -> {
                throw UnsupportedOperationException("Received invalid message: $params")
            }
        }
    }

object Uart {
    private const val UART_FILE = "/dev/ttyS0"
    private var uart: CPointer<GIOChannel>?
    private val fd: Int

    init {
        "Opening UART serial".debug()
        fd = open(UART_FILE, O_RDWR or O_NOCTTY or O_SYNC)
        if (fd == -1) {
            "Error uart opening failed: ${strerror(errno)}".error()
            uart = null
        } else {
            "Serial opened fd: $fd".debug()
            setupSerial(fd)
            uart = g_io_channel_unix_new(fd)
//        g_io_channel_set_buffered(uart, 1/*true*/)
        }
    }

    private fun writeString(message: String): Int = memScoped {
        val buffer = message.encodeToByteArray()
        var written = -1;
        buffer.usePinned {
            written = write(fd, it.addressOf(0), message.length.convert()).toInt()
        }

        if (written == -1) {
            throw UartWriteException("Writing to closed socket")
        }

        tcflush(fd, TCIFLUSH)
        return written

    }

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
                    g_io_channel_read_line(uart, message.ptr, read.ptr, null, error.ptr)
                    error.value?.let {
                        throw UartReadException(it.pointed.message?.toKString() ?: "UART read error without message")
                    }
                    message.value?.let {
                        "UART receive ${it.toKString()}".debug()
                        observable.onNext(UartMessage.fromString(it.toKString()))
                    }
                }
            }
        }
    }.observeOn(ioScheduler)

    fun requestFirmware() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(CMD_FW) }
        }
        writeString(CMD_FW)
    }

    fun requestStatus() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(CMD_STATUS) }
        }
        writeString(CMD_STATUS)
    }

    fun sendHeartBeat() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(CMD_HBT) }
        }
        writeString(CMD_HBT)
    }

    fun requestShutdown() {
        uartQueue.getAndUpdate {
            it.toMutableList().apply { add(CMD_SHUTDOWN) }
        }
        writeString(CMD_SHUTDOWN)
    }

    fun close() {
        uart?.let {
            g_io_channel_close(uart)
        }
    }
}