package cz.fjerabek.thr

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import kotlinx.cinterop.*
import platform.posix.fflush
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fwrite

object Uart {
    const val UART_FILE = "test"
    const val FW_CMD = "\$fwv\r\n"
    const val STATUS_CMD = "\$sta\r\n"
    const val HBT_CMD = "\$hbt\r\n"
    const val SHUTDOWN_CMD = "\$off\r\n"

    private val uartRead = fopen(UART_FILE,"r")
    private val uartWrite= fopen(UART_FILE,"w")

    fun startReceiver()  = observable<String> {observable ->
        val buffer = ByteArray(255) { 0 }
        buffer.usePinned {
            while (true) {
                val read = fgets(it.addressOf(0), 255, uartRead)
                read?.toKString()?.let { it1 -> observable.onNext(it1) };
            }
        }
    }.observeOn(ioScheduler)

    fun requestFirmware() {
        fwrite(FW_CMD.cstr, 1.convert(), FW_CMD.length.convert(), uartWrite)
        fflush(uartWrite)
    }

    fun requestStatus() {
        println("Written: " + fwrite(STATUS_CMD.cstr, 1.convert(), STATUS_CMD.length.convert(), uartWrite))
        fflush(uartWrite)
    }

    fun sendHeartBeat() {
        fwrite(HBT_CMD.cstr, 1.convert(), HBT_CMD.length.convert(), uartWrite)
        fflush(uartWrite)
    }

    fun requestShutdown() {
        fwrite(SHUTDOWN_CMD.cstr, 1.convert(), SHUTDOWN_CMD.length.convert(), uartWrite)
        fflush(uartWrite)
    }
}