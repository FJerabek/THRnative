package cz.fjerabek.thr.midi

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import cz.fjerabek.thr.midi.messages.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.serialization.json.Json
import platform.posix.*

class Midi(port: String) {
    @ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
    fun ByteArray.toHexString() = asUByteArray().joinToString(" ") { it.toString(16).padStart(2, '0') }

    private val json = Json {}

    private val read: CPointer<FILE>? = fopen(port, "rb")
    private val write: CPointer<FILE>? = fopen(port, "wb")

    private fun readMessage() : IMidiMessage {
        var messageStart = false
        val data = arrayListOf<Byte>()
        if (!messageStart) {
            do {
                val byte = fgetc(read)
                if(byte == -1) {
                    throw IllegalStateException("Reading from closed stream")
                }
            } while (byte != 0xF0)
//            data.add(0xF0.toByte())
            messageStart = true
        }

        while (messageStart) {
            val byte = fgetc(read)
            if (byte == 0xF7)
                messageStart = false
            else
                data.add(byte.toByte())
        }

        return createMessage(data.toByteArray()) ?: throw UnsupportedOperationException("Invalid message received")
    }

    private fun createMessage(data: ByteArray) : IMidiMessage? {
        return when {
            data contentEquals heartbeatData -> {
                HeartBeatMessage(data)
            }
            data.size > changePrefix.size &&
                    data.sliceArray(changePrefix.indices) contentEquals changePrefix -> {
                ChangeMessage(data)
            }
            data.size > dumpPrefix.size &&
                    data.sliceArray(dumpPrefix.indices) contentEquals dumpPrefix -> {
                DumpMessage(data)
            }
            else -> null
        }
    }

    fun startMessageReceiver() = observable<IMidiMessage> {
        while(true) {
            it.onNext(readMessage())
        }
    }.observeOn(ioScheduler)

    @ExperimentalUnsignedTypes
    fun sendMessage(message: IMidiMessage) {
//        ioScheduler.submit {
            val data = byteArrayOf(0xF0.toByte()) + message.sysex + byteArrayOf(0xF7.toByte())
            println("Sending: $message   as: ${data.toHexString()}")
            data.usePinned {
                val written = fwrite(it.addressOf(0), 1.convert(), data.size.convert(), write)
                fflush(write)
                if (written.toInt() == 0) {
                    throw IllegalStateException("Writing to closed stream")
                }
            }
//        }
    }

    fun close() {
        fclose(read)
        fclose(write)
    }

    companion object {
        private val dumpPrefix = byteArrayOf(0x43, 0x7D, 0x00, 0x02, 0x0C, 0x44, 0x54, 0x41, 0x31, 0x41, 0x6C, 0x6C, 0x50, 0x00, 0x00, 0x7f, 0x7f)
        private val changePrefix = byteArrayOf(0x43, 0x7D, 0x10, 0x41, 0x30, 0x01)
        val heartbeatData = byteArrayOf(0x43, 0x7D, 0x60, 0x44, 0x54, 0x41, 0x31)
    }
}