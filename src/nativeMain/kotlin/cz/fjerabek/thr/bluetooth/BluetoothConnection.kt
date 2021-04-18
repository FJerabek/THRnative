package cz.fjerabek.thr.bluetooth

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import cz.fjerabek.thr.LogUtils.debug
//import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.warn
import cz.fjerabek.thr.data.bluetooth.IBluetoothMessage
import cz.fjerabek.thr.serializer
import kotlinx.cinterop.*
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import platform.posix.*

@ExperimentalUnsignedTypes
class BluetoothConnection(
    private val socketDescriptor: Int
) {
    init {
        memScoped {
            setToBlockingMode(socketDescriptor)
        }
    }

    private fun setToBlockingMode(fd: Int) {
        var options = fcntl(fd, F_GETFL)
        if (options < 0) {
            throw BluetoothException("Unable to get bluetooth socket options")
        }
        options = options.and(O_NONBLOCK.inv())

        if (fcntl(fd, F_SETFL, options) < 0) {
            throw BluetoothException("Unable to set bluetooth socket options")
        }
    }

    private fun read(): ByteArray {
        val buffer = ByteArray(65535)
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

    fun startReceiver() = observable<IBluetoothMessage> { emitter ->
//        "Starting bluetooth receiver".debug()
        val builder = StringBuilder()
        while (true) {
            try {
                builder.append(readString())
                if (builder.contains('\n')) {
                    val lastMessageDelimiter = builder.lastIndexOf('\n')
                    if(lastMessageDelimiter == -1) {
                        continue
                    }

                    val messages = builder.substring(0..lastMessageDelimiter)
                    builder.deleteRange(0, lastMessageDelimiter + 1)
                    val split = messages.split('\n').filterNot { it.trim().isEmpty() }
                    split.forEach messageSplit@{
                        val message = serializer.decodeFromString(
                            PolymorphicSerializer(IBluetoothMessage::class),
                            it
                        )
                        emitter.onNext(message)
                    }
                }
            } catch (e: SerializationException) {
                "Received unserializable message".warn()
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
        write(message = "$message \n".encodeToByteArray())
    }

    fun sendMessage(message: IBluetoothMessage) {
        writeString(serializer.encodeToString(PolymorphicSerializer(IBluetoothMessage::class), message))
    }
}