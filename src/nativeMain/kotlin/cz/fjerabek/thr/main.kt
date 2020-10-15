@file:Suppress("UNCHECKED_CAST")

package cz.fjerabek.thr

import bluez.register_rfcomm_sdp
import com.badoo.reaktive.observable.*
import com.badoo.reaktive.scheduler.ioScheduler
import cz.fjerabek.thr.bluetooth.Bluetooth
import cz.fjerabek.thr.bluetooth.BluetoothConnection
import cz.fjerabek.thr.controls.Compressor
import cz.fjerabek.thr.controls.Effect
import cz.fjerabek.thr.controls.MainPanel
import cz.fjerabek.thr.controls.Reverb
import cz.fjerabek.thr.controls.compressor.Rack
import cz.fjerabek.thr.controls.compressor.Stomp
import cz.fjerabek.thr.controls.effect.*
import cz.fjerabek.thr.controls.reverb.*
import cz.fjerabek.thr.enums.mainpanel.EAmpType
import cz.fjerabek.thr.enums.mainpanel.ECabinetType
import cz.fjerabek.thr.midi.Midi
import cz.fjerabek.thr.midi.messages.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.native.concurrent.ThreadLocal
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import platform.posix.pread
import platform.posix.random
import platform.posix.sleep
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference


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

@ThreadLocal
val di = DI {
   bind<StringFormat>() with singleton {  Json {
       serializersModule = serializerModule
       prettyPrint = true
   }}
}

@ExperimentalUnsignedTypes
fun ByteArray.toHexString() = asUByteArray().joinToString(" ") { it.toString(16).padStart(2, '0') }

@ThreadLocal
val serializer by di.instance<StringFormat>()

@ExperimentalUnsignedTypes
@SharedImmutable
val bluetoothConnection: AtomicReference<BluetoothConnection?>? = AtomicReference(null)

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
//    while (true) {
//        sleep(5)
//    }
//    Uart.startReceiver().subscribeOn(ioScheduler).subscribe {
//        println(it)
//    }

    while (true) {
        Uart.requestShutdown()
        sleep(2)
    }
}

