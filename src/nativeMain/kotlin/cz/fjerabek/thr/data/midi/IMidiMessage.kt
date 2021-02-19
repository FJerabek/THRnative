package cz.fjerabek.thr.data.midi

import cz.fjerabek.thr.data.bluetooth.IBluetoothMessage

interface IMidiMessage: IBluetoothMessage {
    val sysex: ByteArray
}