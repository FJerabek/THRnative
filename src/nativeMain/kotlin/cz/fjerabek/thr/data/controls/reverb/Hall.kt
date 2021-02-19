package cz.fjerabek.thr.data.controls.reverb

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.reverb.EHall
import cz.fjerabek.thr.data.enums.reverb.EReverb
import cz.fjerabek.thr.data.enums.reverb.EReverbType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Hall")
class Hall(
        override val status: EStatus,
        val time: Int,
        val preDelay: Int,
        val lowCut: Int,
        val highCut: Int,
        val highRatio: Byte,
        val lowRatio: Byte,
        val level: Byte
) : Reverb(EReverbType.SPRING) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EReverb.STATUS.dumpPosition])!!,
            (dump[EHall.TIME.dumpPosition.first] * 128) + dump[EHall.TIME.dumpPosition.second],
            (dump[EHall.PRE_DELAY.dumpPosition.first] * 128) + dump[EHall.PRE_DELAY.dumpPosition.second],
            (dump[EHall.LOW_CUT.dumpPosition.first] * 128) + dump[EHall.LOW_CUT.dumpPosition.second],
            (dump[EHall.HIGH_CUT.dumpPosition.first] * 128) + dump[EHall.HIGH_CUT.dumpPosition.second],
            dump[EHall.HIGH_RATIO.dumpPosition.first],
            dump[EHall.LOW_RATIO.dumpPosition.first],
            dump[EHall.LEVEL.dumpPosition.first]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EHall.TIME.dumpPosition.first] = (time / 128).toByte()
        dump[EHall.TIME.dumpPosition.second] = (time % 128).toByte()

        dump[EHall.PRE_DELAY.dumpPosition.first] = (preDelay / 128).toByte()
        dump[EHall.PRE_DELAY.dumpPosition.second] = (preDelay % 128).toByte()

        dump[EHall.LOW_CUT.dumpPosition.first] = (lowCut / 128).toByte()
        dump[EHall.LOW_CUT.dumpPosition.second] = (lowCut % 128).toByte()

        dump[EHall.HIGH_CUT.dumpPosition.first] = (highCut / 128).toByte()
        dump[EHall.HIGH_CUT.dumpPosition.second] = (highCut % 128).toByte()

        dump[EHall.HIGH_RATIO.dumpPosition.first] = highRatio
        dump[EHall.LOW_RATIO.dumpPosition.first] = lowRatio
        dump[EHall.LEVEL.dumpPosition.first] = level

        return super.toDump(dump)
    }
}