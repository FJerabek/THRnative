package cz.fjerabek.thr.data.midi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Change")
data class ChangeMessage(
    val property: Byte,
    val value : Int
): IMidiMessage {

    constructor(data: ByteArray) : this(data[prefix.size], data[prefix.size + 1] * 128 + data[prefix.size + 2])

    override val sysex
        get() = prefix + byteArrayOf(property) + byteArrayOf((value / 128).toByte(), (value % 128).toByte())

    companion object {
        val prefix = byteArrayOf(0x43, 0x7D, 0x10, 0x41, 0x30, 0x01)
    }
}