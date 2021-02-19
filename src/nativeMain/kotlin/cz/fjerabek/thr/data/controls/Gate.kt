package cz.fjerabek.thr.data.controls

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.gate.EGate
import kotlinx.serialization.Serializable

@Serializable
class Gate(
    val status : EStatus,
    val threshold : Byte,
    val release : Byte
) : IControl {

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EGate.STATUS.dumpPosition] = status.value
        dump[EGate.THRESHOLD.dumpPosition] = threshold
        dump[EGate.RELEASE.dumpPosition] = release
        return dump
    }

    companion object {
        fun fromDump(dump : ByteArray) : Gate {
            return Gate(
                EStatus.fromValue(dump[EGate.STATUS.dumpPosition])!!,
                dump[EGate.THRESHOLD.dumpPosition],
                dump[EGate.RELEASE.dumpPosition])
        }
    }
}