package cz.fjerabek.thr.data.midi

import kotlinx.serialization.Serializable

@Serializable
data class HeartBeatMessage(
        override val sysex: ByteArray = heartbeatData
): IMidiMessage {
    companion object {
        val heartbeatData = byteArrayOf(0x43, 0x7D, 0x60, 0x44, 0x54, 0x41, 0x31)
    }

    override fun equals(other: Any?): Boolean {
        if(other !is HeartBeatMessage) return false
        return other.sysex contentEquals sysex
    }

    override fun hashCode(): Int {
        return sysex.contentHashCode()
    }
}
