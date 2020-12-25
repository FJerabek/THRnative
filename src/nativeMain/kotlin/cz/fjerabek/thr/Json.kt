package cz.fjerabek.thr

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
import cz.fjerabek.thr.midi.messages.ChangeMessage
import cz.fjerabek.thr.midi.messages.HeartBeatMessage
import cz.fjerabek.thr.midi.messages.IMidiMessage
import cz.fjerabek.thr.midi.messages.PresetMessage
import cz.fjerabek.thr.uart.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
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
        subclass(PresetMessage::class)
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
        subclass(PresetMessage::class)
        subclass(ChangeMessage::class)

        subclass(HwStatusRq::class)
        subclass(FwVersionRq::class)
        subclass(PresetsRq::class)
        subclass(PresetsResponse::class)
        subclass(PresetSelect::class)
        subclass(CurrentPresetRq::class)
        subclass(RemovePresetRq::class)
        subclass(AddPresetRq::class)
        subclass(Lamp::class)
        subclass(WideStereo::class)
        subclass(ConnectedRq::class)
        subclass(Connected::class)
    }
}

@ThreadLocal
val serializer = Json {
    serializersModule = serializerModule
    prettyPrint = true
}
