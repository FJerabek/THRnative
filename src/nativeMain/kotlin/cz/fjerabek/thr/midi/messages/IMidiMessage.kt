package cz.fjerabek.thr.midi.messages

import cz.fjerabek.thr.bluetooth.IBluetoothMessage
import kotlinx.serialization.Polymorphic

interface IMidiMessage: IBluetoothMessage{
    val sysex: ByteArray
}