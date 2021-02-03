@file:Suppress("UNCHECKED_CAST")

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
import cz.fjerabek.thr.bluetooth.*
import cz.fjerabek.thr.file.PresetsManager
import cz.fjerabek.thr.glib.GLib
import cz.fjerabek.thr.midi.Midi
import cz.fjerabek.thr.midi.MidiDisconnectedException
import cz.fjerabek.thr.midi.messages.ChangeMessage
import cz.fjerabek.thr.midi.messages.HeartBeatMessage
import cz.fjerabek.thr.midi.messages.IMidiMessage
import cz.fjerabek.thr.midi.messages.PresetMessage
import cz.fjerabek.thr.uart.*
import kotlinx.serialization.ExperimentalSerializationApi
import platform.posix.sleep
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.SharedImmutable

// Midi device
@SharedImmutable
val midi = AtomicReference<Midi?>(null)

//Connected bluetooth device
@ExperimentalUnsignedTypes
@SharedImmutable
val bluetoothConnection: AtomicReference<BluetoothConnection?> = AtomicReference(null)

//Loaded presets
@SharedImmutable
val presets = AtomicReference<List<PresetMessage>>(listOf())

//Midi port file location
@SharedImmutable
val midiPort = AtomicReference("/dev/midi3")

//Preset save file location
@SharedImmutable
val presetsFilePath = AtomicReference("presets.json")

//Current index of selected preset -1 is not saved custom
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
            bluetoothConnection.value?.sendMessage(Connected(false))
            midiConnect(midiPort.value)
            midi.value = null
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
    }.observeOn(ioScheduler)
        .subscribeOn(mainScheduler)
        .subscribe {
            midi.value = it
            bluetoothConnection.value?.sendMessage(Connected(true))
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
        is HwStatusRq -> Uart.requestStatus()
        is PresetsRq -> bluetoothConnection.value?.sendMessage(PresetsResponse(presets.value))
        is CurrentPresetRq -> midi.value?.requestDump()
        is Lamp -> midi.value?.lamp(message.on)
        is WideStereo -> midi.value?.wideStereo(message.on)
        is ConnectedRq -> bluetoothConnection.value?.sendMessage(Connected(midi.value != null))
        is RemovePresetRq -> {
            if (message.index >= presets.value.size) {
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
                it.toMutableList().run {
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
            if (message.index >= presets.value.size) {
                "Invalid preset index: ${message.index} preset size: ${presets.value.size}".error()
            } else {
                midi.value?.sendMessage(presets.value[message.index])
            }
        }
    }
}

/**
 * Called when bluetooth connection is accepted
 * @param connection accepted connection
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
 * @param message received message
 */
fun uartMessageReceived(message: UartMessage) {
    when (message) {
        is ButtonMessage -> {
            if (message.pressed) { //Run on press not release
                when (message.id) {
                    1 -> { //Left button
                        if (currentPreset.value == -1 || currentPreset.value <= 0) {
                            currentPreset.value = presets.value.size - 1
                        } else {
                            currentPreset.value--
                        }
                    }
                    2 -> { //Right button
                        if (currentPreset.value == -1 || currentPreset.value + 1 >= presets.value.size) {
                            currentPreset.value = 0
                        } else {
                            currentPreset.value++
                        }
                    }
                }
                try {
                    midi.value?.sendMessage(presets.value[currentPreset.value])
                    bluetoothConnection.value?.sendMessage(PresetSelect(currentPreset.value))
                } catch (e: MidiDisconnectedException) {
                    onMidiError(e)
                }
            }
        }
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
    if (args.isEmpty()) {
        println("Needed path to midi device")
        return
    }
    midiPort.value = args[0]

//    presets.update {
//        PresetsManager.loadPresets(presetsFilePath.value).toMutableList().let {
//            "Loaded ${it.size} presets".info()
//            it
//        }
//    }

    bluetoothConnect()
    midiConnect(midiPort.value)
    setupUartReceiver()

    while (true) {
        GLib.send()
        sleep(60)
    }
}
