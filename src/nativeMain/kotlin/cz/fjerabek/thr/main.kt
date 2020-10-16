@file:Suppress("UNCHECKED_CAST")

package cz.fjerabek.thr

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.observable.subscribe
import com.badoo.reaktive.observable.subscribeOn
import com.badoo.reaktive.scheduler.ioScheduler
import cz.fjerabek.thr.bluetooth.Bluetooth
import cz.fjerabek.thr.bluetooth.BluetoothConnection
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
import kotlin.native.concurrent.AtomicReference
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
}

@ExperimentalUnsignedTypes
fun ByteArray.toHexString() = asUByteArray().joinToString(" ") { it.toString(16).padStart(2, '0') }

@ThreadLocal
val serializer = Json {
    serializersModule = serializerModule
    prettyPrint = true
}

@ExperimentalUnsignedTypes
@SharedImmutable
val bluetoothConnection: AtomicReference<BluetoothConnection?> = AtomicReference(null)

@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
//    if(args.isEmpty()) {
//        println("Needed path to midi device")
//    }
//    val bluetoothConnector = observable<BluetoothConnection> {
//        val connection = Bluetooth.acceptConnection()
//        it.onNext(connection)
//        connection.startReceiver().subscribe { message ->
//            println(message)
//        }
//    }.observeOn(ioScheduler)
//
//    val midi = Midi(args[0])
//
//    val observable = midi.startMessageReceiver()
//    observable.subscribeOn(ioScheduler)
//            .subscribe {
//                if(it is HeartBeatMessage) return@subscribe
//                bluetoothConnection?.value?.run {
//                    writeString(serializer.encodeToString(PolymorphicSerializer(IMidiMessage::class), it))
//                }
//            }
//
//    bluetoothConnector.subscribeOn(ioScheduler).subscribe {
//        bluetoothConnection?.value = it
//    }
//
//    Uart.startReceiver().subscribeOn(ioScheduler).subscribe {
//        when(it) {
//            is ButtonMessage -> println(it)
//            is FWVersionMessage -> println(it)
//            is StatusMessage -> println(it)
//            is ShutdownMessage -> println(it)
//        }
//    }



    while (true) {
        sleep(2)
//        println("REQUEST")
//        Uart.requestStatus()
//        Uart.requestFirmware()
    }

}

