package cz.fjerabek.thr.midi

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.newThreadScheduler
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.midi.messages.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.*

class MidiDisconnectedException(message: String) : Exception(message)
class MidiUnknownMessageException(message: String) : Exception(message)

class Midi(port: String) {
    @ExperimentalUnsignedTypes // just to make it clear that the experimental unsigned types are used
    fun ByteArray.toHexString() = asUByteArray().joinToString(" ") { it.toString(16).padStart(2, '0') }

    init {
        if (access(port, F_OK) != 0) {
            throw IllegalStateException("Midi device not connected")
        }
    }

    private val read: CPointer<FILE>? = fopen(port, "rb")
    private val write: CPointer<FILE>? = fopen(port, "wb")

    private fun readMessage(): IMidiMessage {
        var messageStart = false
        val data = arrayListOf<Byte>()
        if (!messageStart) {
            do {
                val byte = fgetc(read)
                if (byte == -1) {
                    throw MidiDisconnectedException("Reading from closed stream")
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

        return createMessage(data.toByteArray()) ?: throw MidiUnknownMessageException("Invalid message received")
    }

    private fun createMessage(data: ByteArray): IMidiMessage? {
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
                PresetMessage(data)
            }
            else -> null
        }
    }

    fun startMessageReceiver() = observable<IMidiMessage> {
        while (true) {
            it.onNext(readMessage())
        }
    }.observeOn(newThreadScheduler)

    private fun send(data: ByteArray) {
        val midiData = byteArrayOf(0xF0.toByte()) + data + byteArrayOf(0xF7.toByte())
        "Midi sending data: ${midiData.toHexString()}".debug()
        midiData.usePinned {
            val written = fwrite(it.addressOf(0), 1.convert(), midiData.size.convert(), write)
            fflush(write)
            if (written.toInt() == 0) {
                throw MidiDisconnectedException("Sending message to closed socket")
            }
        }
    }

    @ExperimentalUnsignedTypes
    fun sendMessage(message: IMidiMessage) {
        send(message.sysex)
    }

    fun requestDump() {
        val dumpCommand = byteArrayOf(
            0x43, 0x7D, 0x20, 0x44, 0x54, 0x41, 0x31, 0x41, 0x6C, 0x6C, 0x50,
        )
        send(dumpCommand)
    }

    fun close() {
        fclose(read)
        fclose(write)
    }

    companion object {
        private val dumpPrefix = byteArrayOf(
            0x43,
            0x7D,
            0x00,
            0x02,
            0x0C,
            0x44,
            0x54,
            0x41,
            0x31,
            0x41,
            0x6C,
            0x6C,
            0x50,
            0x00,
            0x00,
            0x7f,
            0x7f
        )
        private val changePrefix = byteArrayOf(0x43, 0x7D, 0x10, 0x41, 0x30, 0x01)
        val heartbeatData = byteArrayOf(0x43, 0x7D, 0x60, 0x44, 0x54, 0x41, 0x31)
        const val DISCOVERY_DELAY = 1u

        fun waitForConnection(port: String): Midi {
            while (true) {
                kotlin.runCatching {
                    Midi(port)
                }.onFailure {
                    sleep(DISCOVERY_DELAY)
                }.onSuccess {
                    "Midi connected".debug()
                    return it
                }
            }
        }
    }
}