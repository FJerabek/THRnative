package cz.fjerabek.thr.data.controls

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.delay.EDelay
import kotlinx.serialization.Serializable

@Serializable
class Delay(
    val status : EStatus,
    val time : Int,
    val feedback : Byte,
    val highCut : Int,
    val lowCut : Int,
    val level : Byte
) : IControl {

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EDelay.STATUS.dumpPosition.first] = status.value
        dump[EDelay.TIME.dumpPosition.first] = (time / 128).toByte()
        dump[EDelay.TIME.dumpPosition.second] = (time % 128).toByte()
        dump[EDelay.FEEDBACK.dumpPosition.first] = feedback
        dump[EDelay.HIGH_CUT.dumpPosition.first] = (highCut / 128).toByte()
        dump[EDelay.HIGH_CUT.dumpPosition.second] = (highCut % 128).toByte()
        dump[EDelay.LOW_CUT.dumpPosition.first] = (lowCut / 128).toByte()
        dump[EDelay.LOW_CUT.dumpPosition.second] = (lowCut % 128).toByte()
        dump[EDelay.LEVEL.dumpPosition.first] = level

        return dump
    }

    companion object {
        fun fromDump(dump : ByteArray) : Delay {

            return Delay(
                EStatus.fromValue(dump[EDelay.STATUS.dumpPosition.first])!!,
                (dump[EDelay.TIME.dumpPosition.first] * 128) + dump[EDelay.TIME.dumpPosition.second],
                dump[EDelay.FEEDBACK.dumpPosition.first],
                (dump[EDelay.HIGH_CUT.dumpPosition.first] * 128) + dump[EDelay.HIGH_CUT.dumpPosition.second],
                (dump[EDelay.LOW_CUT.dumpPosition.first] * 128) + dump[EDelay.LOW_CUT.dumpPosition.second],
                dump[EDelay.LEVEL.dumpPosition.first]
            )
        }
    }
}