package cz.fjerabek.thr.bluetooth
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.close
import platform.posix.read
import platform.posix.write
import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler

@ExperimentalUnsignedTypes
class BluetoothConnection(
        private val socketDescriptor: Int
) {
    private fun read() : ByteArray{
        val buffer = ByteArray(2048)
        var readBytes = 0L
        buffer.usePinned {
            readBytes = read(socketDescriptor, it.addressOf(0), buffer.size.convert()).toLong()
        }
        if(readBytes < 0) throw IllegalStateException("Socket is closed.")
        return buffer.copyOfRange(0, readBytes.toInt())
    }


    private fun readString() : String{
        return read().toKString()
    }

    fun startReceiver() = observable<String> {
        kotlin.runCatching {
            while(true) {
                it.onNext(readString())
            }
        }
    }.observeOn(ioScheduler)

    fun write(message: ByteArray) {
        var bytesWritten = 0L
        message.usePinned {
            bytesWritten = write(socketDescriptor, it.addressOf(0), message.size.convert()).toLong()
        }
        if(bytesWritten < 0) throw IllegalStateException("Socket is closed.")
    }

    fun writeString(message: String) {
        write(message = message.encodeToByteArray())
    }

    fun close() {
        close(socketDescriptor)
    }
}