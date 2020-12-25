@file:Suppress("UNCHECKED_CAST", "EXPERIMENTAL_UNSIGNED_TYPES")

package cz.fjerabek.thr

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.observable.subscribe
import com.badoo.reaktive.observable.subscribeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.badoo.reaktive.scheduler.mainScheduler
import com.badoo.reaktive.utils.atomic.AtomicReference
import com.badoo.reaktive.utils.atomic.update
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.LogUtils.info
import cz.fjerabek.thr.LogUtils.warn
import cz.fjerabek.thr.bluetooth.*
import cz.fjerabek.thr.file.PresetsManager
import cz.fjerabek.thr.midi.Midi
import cz.fjerabek.thr.midi.MidiDisconnectedException
import cz.fjerabek.thr.midi.MidiUnknownMessageException
import cz.fjerabek.thr.midi.messages.ChangeMessage
import cz.fjerabek.thr.midi.messages.HeartBeatMessage
import cz.fjerabek.thr.midi.messages.IMidiMessage
import cz.fjerabek.thr.midi.messages.PresetMessage
import cz.fjerabek.thr.uart.*
import kotlinx.serialization.ExperimentalSerializationApi
import platform.posix.sleep
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
val midi = AtomicReference<Midi?>(null)

@ExperimentalUnsignedTypes
@SharedImmutable
val bluetoothConnection: AtomicReference<BluetoothConnection?> = AtomicReference(null)

@SharedImmutable
val presets = AtomicReference<List<PresetMessage>>(listOf())

@SharedImmutable
val midiPort = AtomicReference("/dev/midi3")

@SharedImmutable
val presetsFilePath = AtomicReference("presets.json")

@SharedImmutable
val currentPreset = AtomicInt(-1)

@ExperimentalUnsignedTypes
fun ByteArray.toHexString() = asUByteArray().joinToString(" ") { it.toString(16).padStart(2, '0') }

/**
 * Called on midi message receive
 */
@ExperimentalUnsignedTypes
fun onMidiMessage(message: IMidiMessage) {
    if (message is HeartBeatMessage) return
    if (message is ChangeMessage && currentPreset.value != -1) {
        currentPreset.value = -1
        bluetoothConnection.value?.run {
            sendMessage(PresetSelect(-1))
        }
    }
    bluetoothConnection.value?.run {
        try {
            sendMessage(message)
        } catch (e: BluetoothConnectionClosedException) {
            bluetoothError(e)
        }
    }
}

/**
 * Called on midi read/write error
 */
fun onMidiError(throwable: Throwable) {
    when (throwable) {
        is MidiDisconnectedException -> {
            "MIDI Disconnected".info()
            midi.value?.close()
            midiConnect(midiPort.value)
        }
        else -> "Midi error ${throwable.stackTraceToString()}".error()
    }
}
/**
 * Starts connecting to THR midi device. Blocks current thread if not connected
 */
fun midiConnect(port: String) {
    observable<Midi> {
        it.onNext(Midi.waitForConnection(port))
    }.observeOn(ioScheduler).subscribeOn(mainScheduler).subscribe {
        midi.value = it
        it.startMessageReceiver()
            .subscribeOn(mainScheduler)
            .subscribe(onError = ::onMidiError, onNext = ::onMidiMessage)
    }
}

/**
 * Called on bluetooth read/write error
 */
fun bluetoothError(e: Throwable) {
    when (e) {
        is BluetoothConnectionClosedException -> {
            "Bluetooth connection closed".info()
            bluetoothConnection.value?.close()
            bluetoothConnection.value = null
            bluetoothConnect()
        }
        else -> "Bluetooth error: ${e.stackTraceToString()}".error()
    }
}

/**
 * Called when bluetooth message received
 */
fun bluetoothMessage(message: IBluetoothMessage) {
    "Bluetooth received: $message".debug()
    when (message) {
        is IMidiMessage -> midi.value?.sendMessage(message)
        is FwVersionRq -> Uart.requestFirmware()
        is HwStatusRq ->  Uart.requestStatus()
        is PresetsRq -> bluetoothConnection.value?.sendMessage(PresetsResponse(presets.value))
        is CurrentPresetRq -> midi.value?.requestDump()
        is RemovePresetRq -> {
            if(message.index >= presets.value.size) {
                "Invalid preset index: ${message.index} preset size: ${presets.value.size}".error()
            } else {
                presets.update {
                    it.filterIndexed { index, _ -> index != message.index }
                }
                PresetsManager.savePresets(presetsFilePath.value, presets.value)
            }
        }
        is AddPresetRq -> {
            presets.update {
                it.toMutableList().run{
                    add(message.preset)
                    this
                }
            }
            PresetsManager.savePresets(presetsFilePath.value, presets.value)
        }
        is SetPresetsRq -> {
            presets.value = message.presets.toMutableList()
            PresetsManager.savePresets(presetsFilePath.value, presets.value)
        }
        is PresetSelect -> {
            if(message.index >= presets.value.size) {
                "Invalid preset index: ${message.index} preset size: ${presets.value.size}".error()
            } else {
                midi.value?.sendMessage(presets.value[message.index])
            }
        }
    }
}

/**
 * Called when bluetooth connection is accepted
 */
fun bluetoothConnection(connection: BluetoothConnection) {
    "Bluetooth connected".info()
    bluetoothConnection.value = connection
    connection.startReceiver()
        .subscribeOn(ioScheduler)
        .subscribe(onError = ::bluetoothError, onNext = ::bluetoothMessage)
}

/**
 * Starts bluetooth connection receiver on io thread
 */
fun bluetoothConnect() {
    observable<BluetoothConnection> {
        val connection = Bluetooth.acceptConnection()
        it.onNext(connection)
    }.observeOn(ioScheduler)
        .subscribeOn(ioScheduler)
        .subscribe(onError = ::bluetoothError, onNext = ::bluetoothConnection)
}

/**
 * Called on uart message received
 */
fun uartMessageReceived(message: UartMessage) {
    when (message) {
        is ButtonMessage -> debug { message }
        is FWVersionMessage -> {
            bluetoothConnection.value?.sendMessage(message)
        }
        is StatusMessage ->
            bluetoothConnection.value?.sendMessage(message)

        is ShutdownMessage -> debug { message }
    }
}

/**
 * Sets up uart receiver
 */
fun setupUartReceiver() {
    Uart.startReceiver()
        .subscribeOn(ioScheduler)
        .subscribe(onNext = ::uartMessageReceived, onError = {
            when (it) {
                is UartReadException -> "Unable to read from uart".error()
                is UartWriteException -> "Unable to write to uart".error()
                else -> "Uart error: ${it.stackTraceToString()}".error()
            }
        })
}

@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
//    if (args.isEmpty()) {
//        println("Needed path to midi device")
//        return
//    }
    midiPort.value = "/dev/midi3"

    presets.update {
        PresetsManager.loadPresets(presetsFilePath.value).toMutableList().let {
            "Loaded ${it.size} presets".debug()
            it
        }
    }

    bluetoothConnect()
    midiConnect(midiPort.value)
    setupUartReceiver()

    while (true) {
        repeat(4) {
            sleep(2)
//        bluetoothConnection.value?.run {
//            sendMessage(HwStatusRq())
//        }
//        Uart.requestStatus()
//        Uart.requestFirmware()
//            midi.value?.requestDump()
        }
    }
}