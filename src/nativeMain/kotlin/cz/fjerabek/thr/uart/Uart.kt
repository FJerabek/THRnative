package cz.fjerabek.thr.uart

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.badoo.reaktive.scheduler.newThreadScheduler
import com.badoo.reaktive.utils.atomic.AtomicReference
import com.badoo.reaktive.utils.atomic.getAndUpdate
import termios.setupSerial
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import glib.*
import kotlinx.cinterop.*
import platform.posix.*

open class UartException(message: String) : Exception(message)
class UartWriteException(message: String) : UartException(message)
class UartReadException(message: String) : UartException(message)

@SharedImmutable
val uartQueue = AtomicReference<List<String>>(listOf())

object Uart {
    private const val UART_FILE = "/dev/ttyS0"
    const val CMD_FW = "\$fwv\r\n"
    const val CMD_STATUS = "\$sta\r\n"
    const val CMD_HBT = "\$hbt\r\n"
    const val CMD_SHUTDOWN = "\$off\r\n"

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