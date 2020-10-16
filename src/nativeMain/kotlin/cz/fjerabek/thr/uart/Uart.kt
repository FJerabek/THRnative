package cz.fjerabek.thr.uart

import bluez.setupSerial
import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.*

object Uart {
    private const val UART_FILE = "/dev/ttyS0"
    private const val CMD_FW = "\$fwv\r\n"
    private const val CMD_STATUS = "\$sta\r\n"
    private const val CMD_HBT = "\$hbt\r\n"
    private const val CMD_SHUTDOWN = "\$off\r\n"

    private val uart = open(UART_FILE, O_RDWR or O_NOCTTY)

    init {
        setupSerial(this.uart)
        setupSerial(this.uart)
    }

    private fun writeString(message: String): Int {
        val buffer = message.encodeToByteArray()
        var written = -1;
        buffer.usePinned {
            written = write(this.uart, it.addressOf(0), message.length.convert()).toInt()
        }

        if(written == -1) {
            throw IllegalStateException("Writing to closed socket")
        }

        tcflush(this.uart, TCIFLUSH)
        return written
    }

    fun startReceiver()  = observable<UartMessage> {observable ->
        val buffer = ByteArray(255) { 0 }
        val message = StringBuilder()
        buffer.usePinned { pinnedBuffer ->
            while (true) {
                val read = read(this.uart, pinnedBuffer.addressOf(0), 255)

                if(read.toInt() == -1)
                    throw IllegalStateException("Reading from closed socket")
                else {
                    repeat(read.toInt()) {
                        when(val char = buffer[it].toChar()) {
                            '\n' -> {
                                observable.onNext(
                                    UartMessage.fromString(
                                        message.toString()
                                    )
                                )
                                message.clear()
                            }
                            else -> message.append(char)
                        }
                    }
                }
            }
        }
    }.observeOn(ioScheduler)

    fun requestFirmware() {
//        uartQueue.value.addLast(EMessageType.FIRMWARE_VERSION)
        writeString(CMD_FW)
    }

    fun requestStatus() {
//        uartQueue.value.addLast(EMessageType.STATUS)
        writeString(CMD_STATUS)
    }

    fun sendHeartBeat() {
        writeString(CMD_HBT)
    }

    fun requestShutdown() {
//        uartQueue.value.addLast(EMessageType.SHUTDOWN)
        writeString(CMD_SHUTDOWN)
    }
}