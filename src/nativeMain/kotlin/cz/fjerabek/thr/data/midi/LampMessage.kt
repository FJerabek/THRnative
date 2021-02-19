package cz.fjerabek.thr.data.midi

import kotlinx.serialization.Serializable

@Serializable
class LampMessage(var status : Boolean): IMidiMessage {
    override val sysex: ByteArray
        get() = byteArrayOf(0x43, 0x7D, 0x30, 0x41, 0x30, 0x01, if(status) 0x01 else 0x00)
}