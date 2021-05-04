package cz.fjerabek.thr.midi

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.newThreadScheduler
import cz.fjerabek.thr.LogUtils.info
import cz.fjerabek.thr.LogUtils.warn
import cz.fjerabek.thr.data.midi.ChangeMessage
import cz.fjerabek.thr.data.midi.HeartBeatMessage
import cz.fjerabek.thr.data.midi.IMidiMessage
import cz.fjerabek.thr.data.midi.PresetMessage
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.*

/**
 * Exception thrown when midi device is disconnected
 * @param message exception message
 */
class MidiDisconnectedException(message: String) : Exception(message)

/**
 * Exception when midi receives unknown message
 * @param message exception message
 */
class MidiUnknownMessageException(message: String) : Exception(message)

/**
 * Class representing midi device
 * @param port midi /dev file for communication
 */
class Midi(port: String) {

    init {
        if (access(port, F_OK) != 0) {
            throw IllegalStateException("Midi device not connected")
        }
    }

    private val read: CPointer<FILE>? = fopen(port, "rb")
    private val write: CPointer<FILE>? = fopen(port, "wb")

    /**
     * Reads message from midi
     * @throws MidiDisconnectedException when trying to read from disconnected device
     * @return Read message
     */
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

    /**
     * Creates correct message type from byte array data
     * @return created message
     */
    private fun createMessage(data: ByteArray): IMidiMessage? {
        return when {
            data contentEquals HeartBeatMessage.heartbeatData -> {
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

    /**
     * Starts midi message receiver
     * @return Observable called when new midi message is received
     */
    fun startMessageReceiver() = observable<IMidiMessage> {
        while (true) {
            try {
                it.onNext(readMessage())
            } catch (e: MidiUnknownMessageException) {
                "Received unknown midi message".warn()
            }
        }
    }.observeOn(newThreadScheduler)

    /**
     * Sends data to midi device
     * @param data data to send
     */
    private fun send(data: ByteArray) {
        val midiData = byteArrayOf(0xF0.toByte()) + data + byteArrayOf(0xF7.toByte())
        midiData.usePinned {
            val written = fwrite(it.addressOf(0), 1.convert(), midiData.size.convert(), write)
            fflush(write)
            if (written.toInt() == 0) {
                throw MidiDisconnectedException("Sending message to closed socket")
            }
        }
    }

    /**
     * Sends midi message to device
     * @param message midi message to send
     */
    @ExperimentalUnsignedTypes
    fun sendMessage(message: IMidiMessage) {
        send(message.sysex)
    }

    /**
     * Sends dump request to midi device
     */
    fun requestDump() {
        val dumpCommand = byteArrayOf(
            0x43, 0x7D, 0x20, 0x44, 0x54, 0x41, 0x31, 0x41, 0x6C, 0x6C, 0x50,
        )
        send(dumpCommand)
    }

    /**
     * Sends lamp settings to device
     */
    fun lamp(on: Boolean) {
        val lamp = byteArrayOf(0xF0.toByte(), 0x43, 0x7D, 0x30, 0x41, 0x30, 0x01, if(on) 0x00 else 0x01, 0xF7.toByte())
        send(lamp)
    }

    /**
     * Sends wide stereo settings to device
     */
    fun wideStereo(on: Boolean) {
        val wideStereo = byteArrayOf(0xF0.toByte(), 0x43, 0x7D, 0x30, 0x41, 0x30, 0x00, if(on) 0x00 else 0x01, 0xF7.toByte())
        send(wideStereo)
    }

    /**
     * Closes all connection to device
     */
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
        const val DISCOVERY_DELAY = 1u

        /**
         * Blocks the thread until midi connection is available
         * @param port path to midi /dev file
         * @return midi connection
         */
        fun waitForConnection(port: String): Midi {
            while (true) {
                kotlin.runCatching {
                    Midi(port)
                }.onFailure {
                    sleep(DISCOVERY_DELAY)
                }.onSuccess {
                    "Midi connected".info()
                    return it
                }
            }
        }
    }
}