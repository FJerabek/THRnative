package cz.fjerabek.thr.midi.messages

import cz.fjerabek.thr.midi.Midi
import kotlinx.serialization.Serializable

@Serializable
data class HeartBeatMessage(
        override val sysex: ByteArray = Midi.heartbeatData
): IMidiMessage {

    override fun equals(other: Any?): Boolean {
        if(other !is HeartBeatMessage) return false
        return other.sysex contentEquals sysex
    }

    override fun hashCode(): Int {
        return sysex.contentHashCode()
    }
}
