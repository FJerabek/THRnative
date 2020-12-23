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
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.serializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromString
import platform.posix.sockaddr

@ExperimentalUnsignedTypes
class BluetoothConnection(
    private val socketDescriptor: Int
) {
    private fun read(): ByteArray {
        val buffer = ByteArray(2048)
        var readBytes = 0L
        buffer.usePinned {
            readBytes = read(socketDescriptor, it.addressOf(0), buffer.size.convert()).toLong()
        }
        if (readBytes < 0) throw BluetoothConnectionClosedException("Trying to read from closed connection")
        return buffer.copyOfRange(0, readBytes.toInt())
    }


    private fun readString(): String {
        return read().toKString()
    }

    fun startReceiver() = observable<IBluetoothMessage> {
        kotlin.runCatching {
            while (true) {
                //Fixme: On error message receiver is stopped.
                it.onNext(
                    serializer.decodeFromString(
                        PolymorphicSerializer(IBluetoothMessage::class),
                        readString()
                    )
                )
            }
        }
    }.observeOn(ioScheduler)

    fun write(message: ByteArray) {
        var bytesWritten = 0L
        message.usePinned {
            bytesWritten = write(socketDescriptor, it.addressOf(0), message.size.convert()).toLong()
        }
        if (bytesWritten < 0) throw BluetoothConnectionClosedException("Trying to write to closed connection")
    }

    fun writeString(message: String) {
        write(message = message.encodeToByteArray())
    }

    fun sendMessage(message: IBluetoothMessage) {
        writeString(serializer.encodeToString(PolymorphicSerializer(IBluetoothMessage::class), message))
    }

    fun close() {
        close(socketDescriptor)
    }
}