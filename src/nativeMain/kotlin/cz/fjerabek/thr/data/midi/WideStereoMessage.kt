package cz.fjerabek.thr.data.midi

import kotlinx.serialization.Serializable

@Serializable
class WideStereoMessage(var status : Boolean): IMidiMessage {
    override val sysex: ByteArray
        get() = byteArrayOf(0x43, 0x7D, 0x30, 0x41, 0x30, 0x00, if(status) 0x00 else 0x01)
}