@file:Suppress("UNCHECKED_CAST", "EXPERIMENTAL_UNSIGNED_TYPES")

package cz.fjerabek.thr

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.observable.subscribe
import com.badoo.reaktive.observable.subscribeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.badoo.reaktive.scheduler.mainScheduler
import com.badoo.reaktive.utils.atomic.AtomicReference
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.LogUtils.info
import cz.fjerabek.thr.LogUtils.warn
import cz.fjerabek.thr.bluetooth.*
import cz.fjerabek.thr.controls.Compressor
import cz.fjerabek.thr.controls.Effect
import cz.fjerabek.thr.controls.Reverb
import cz.fjerabek.thr.controls.compressor.Rack
import cz.fjerabek.thr.controls.compressor.Stomp
import cz.fjerabek.thr.controls.effect.Chorus
import cz.fjerabek.thr.controls.effect.Flanger
import cz.fjerabek.thr.controls.effect.Phaser
import cz.fjerabek.thr.controls.effect.Tremolo
import cz.fjerabek.thr.controls.reverb.Hall
import cz.fjerabek.thr.controls.reverb.Plate
import cz.fjerabek.thr.controls.reverb.Room
import cz.fjerabek.thr.controls.reverb.Spring
import cz.fjerabek.thr.midi.Midi
import cz.fjerabek.thr.midi.MidiDisconnectedException
import cz.fjerabek.thr.midi.MidiUnknownMessageException
import cz.fjerabek.thr.midi.messages.ChangeMessage
import cz.fjerabek.thr.midi.messages.DumpMessage
import cz.fjerabek.thr.midi.messages.HeartBeatMessage
import cz.fjerabek.thr.midi.messages.IMidiMessage
import cz.fjerabek.thr.uart.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import platform.posix.sleep
import kotlin.native.concurrent.SharedImmutable
import kotlin.native.concurrent.ThreadLocal


@ThreadLocal
val serializerModule = SerializersModule {
    polymorphic(Effect::class) {
        subclass(Chorus::class)
        subclass(Flanger::class)
        subclass(Phaser::class)
        subclass(Tremolo::class)
    }

    polymorphic(Compressor::class) {
        subclass(Rack::class)
        subclass(Stomp::class)
    }

    polymorphic(Reverb::class) {
        subclass(Hall::class)
        subclass(Plate::class)
        subclass(Room::class)
        subclass(Spring::class)
    }

    polymorphic(IMidiMessage::class) {
        subclass(HeartBeatMessage::class)
        subclass(DumpMessage::class)
        subclass(ChangeMessage::class)
    }

    polymorphic(UartMessage::class) {
        subclass(ButtonMessage::class)
        subclass(FWVersionMessage::class)
        subclass(StatusMessage::class)
        subclass(ShutdownMessage::class)
    }

    polymorphic(IBluetoothMessage::class) {
        subclass(ButtonMessage::class)
        subclass(FWVersionMessage::class)
        subclass(StatusMessage::class)
        subclass(ShutdownMessage::class)
        subclass(HeartBeatMessage::class)
        subclass(DumpMessage::class)
        subclass(ChangeMessage::class)

        subclass(HwStatusRq::class)
        subclass(FwVersionRq::class)
    }
}

@ThreadLocal
val serializer = Json {
    serializersModule = serializerModule
    prettyPrint = true
}

@ThreadLocal
var midi: Midi? = null

@ExperimentalUnsignedTypes
@SharedImmutable
val bluetoothConnection: AtomicReference<BluetoothConnection?> = AtomicReference(null)

@SharedImmutable
val midiPort = AtomicReference("/dev/midi3")

@ExperimentalUnsignedTypes
fun ByteArray.toHexString() = asUByteArray().joinToString(" ") { it.toString(16).padStart(2, '0') }


/**
 * Called on midi message receive
 */
@ExperimentalUnsignedTypes
fun onMidiMessage(message: IMidiMessage) {
    if (message is HeartBeatMessage) return
    bluetoothConnection.value?.run {
        try {
            sendMessage(message)
        } catch (e: BluetoothConnectionClosedException) {
            // Trying to write to closed bluetooth connection
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
            info {
                "MIDI Disconnected"
            }
            midi?.close()
            midiConnect(midiPort.value)
        }
        is MidiUnknownMessageException -> {
            warn {
                "MIDI Received unknown message"
            }
        }
        else -> {
            throwable.printStackTrace()
        }
    }
}

/**
 * Starts connecting to THR midi device. Blocks current thread if not connected
 */
fun midiConnect(port: String) {
    observable<Midi> {
        it.onNext(Midi.waitForConnection(port))
    }.observeOn(ioScheduler).subscribeOn(mainScheduler).subscribe {
        midi = it
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
            info {
                "Bluetooth connection closed"
            }
            bluetoothConnection.value?.close()
            bluetoothConnection.value = null
            bluetoothConnect()
        }
        is BluetoothUnknownMessageException -> {
            //Fixme: Gets called only once and then is ignored
            warn { "Received unserializable bluetooth message" }
            bluetoothConnection.value?.startReceiver()
        }
        else -> error {
            "Bluetooth error: ${e.stackTraceToString()}"
        }
    }
}

/**
 * Called when bluetooth message received
 */
fun bluetoothMessage(message: IBluetoothMessage) {
    debug { "Bluetooth received: $message" }
    when(message){
        is FwVersionRq -> {
            Uart.requestFirmware()
        }
        is HwStatusRq -> {
            Uart.requestStatus()
        }
        //Todo: Add more bluetooth request messages
    }
}

/**
 * Called when bluetooth connection is accepted
 */
fun bluetoothConnection(connection: BluetoothConnection) {
    debug { "Bluetooth connected" }
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
            error {
                "Uart error: ${it.stackTraceToString()}"
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

    bluetoothConnect()
    midiConnect(midiPort.value)
    setupUartReceiver()

    while (true) {
        sleep(2)
//        bluetoothConnection.value?.run {
//            sendMessage(HwStatusRq())
//        }
//        Uart.requestStatus()
//        Uart.requestFirmware()
    }
}