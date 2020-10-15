package cz.fjerabek.thr.midi.messages

import kotlinx.serialization.Polymorphic

interface IMidiMessage {
    val sysex: ByteArray
}