package cz.fjerabek.thr.data.controls.reverb

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.reverb.EReverb
import cz.fjerabek.thr.data.enums.reverb.EReverbType
import cz.fjerabek.thr.data.enums.reverb.ERoom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Room")
class Room(
        override val status: EStatus,
        val time: Int,
        val preDelay: Int,
        val lowCut: Int,
        val highCut: Int,
        val highRatio: Byte,
        val lowRatio: Byte,
        val level: Byte
) : Reverb(EReverbType.ROOM) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EReverb.STATUS.dumpPosition])!!,
            (dump[ERoom.TIME.dumpPosition.first] * 128) + dump[ERoom.TIME.dumpPosition.second],
            (dump[ERoom.PRE_DELAY.dumpPosition.first] * 128) + dump[ERoom.PRE_DELAY.dumpPosition.second],
            (dump[ERoom.LOW_CUT.dumpPosition.first] * 128) + dump[ERoom.LOW_CUT.dumpPosition.second],
            (dump[ERoom.HIGH_CUT.dumpPosition.first] * 128) + dump[ERoom.HIGH_CUT.dumpPosition.second],
            dump[ERoom.HIGH_RATIO.dumpPosition.first],
            dump[ERoom.LOW_RATIO.dumpPosition.first],
            dump[ERoom.LEVEL.dumpPosition.first]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[ERoom.TIME.dumpPosition.first] = (time / 128).toByte()
        dump[ERoom.TIME.dumpPosition.second] = (time % 128).toByte()

        dump[ERoom.PRE_DELAY.dumpPosition.first] = (preDelay / 128).toByte()
        dump[ERoom.PRE_DELAY.dumpPosition.second] = (preDelay % 128).toByte()

        dump[ERoom.LOW_CUT.dumpPosition.first] = (lowCut / 128).toByte()
        dump[ERoom.LOW_CUT.dumpPosition.second] = (lowCut % 128).toByte()

        dump[ERoom.HIGH_CUT.dumpPosition.first] = (highCut / 128).toByte()
        dump[ERoom.HIGH_CUT.dumpPosition.second] = (highCut % 128).toByte()

        dump[ERoom.HIGH_RATIO.dumpPosition.first] = highRatio
        dump[ERoom.LOW_RATIO.dumpPosition.first] = lowRatio
        dump[ERoom.LEVEL.dumpPosition.first] = level

        return super.toDump(dump)
    }
}