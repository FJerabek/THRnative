package cz.fjerabek.thr.data.controls.reverb

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.reverb.EPlate
import cz.fjerabek.thr.data.enums.reverb.EReverb
import cz.fjerabek.thr.data.enums.reverb.EReverbType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("Plate")
class Plate(
        override val status: EStatus,
        val time: Int,
        val preDelay: Int,
        val lowCut: Int,
        val highCut: Int,
        val highRatio: Byte,
        val lowRatio: Byte,
        val level: Byte
) : Reverb(EReverbType.PLATE) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EReverb.STATUS.dumpPosition])!!,
            (dump[EPlate.TIME.dumpPosition.first] * 128) + dump[EPlate.TIME.dumpPosition.second],
            (dump[EPlate.PRE_DELAY.dumpPosition.first] * 128) + dump[EPlate.PRE_DELAY.dumpPosition.second],
            (dump[EPlate.LOW_CUT.dumpPosition.first] * 128) + dump[EPlate.LOW_CUT.dumpPosition.second],
            (dump[EPlate.HIGH_CUT.dumpPosition.first] * 128) + dump[EPlate.HIGH_CUT.dumpPosition.second],
            dump[EPlate.HIGH_RATIO.dumpPosition.first],
            dump[EPlate.LOW_RATIO.dumpPosition.first],
            dump[EPlate.LEVEL.dumpPosition.first]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EPlate.TIME.dumpPosition.first] = (time / 128).toByte()
        dump[EPlate.TIME.dumpPosition.second] = (time % 128).toByte()

        dump[EPlate.PRE_DELAY.dumpPosition.first] = (preDelay / 128).toByte()
        dump[EPlate.PRE_DELAY.dumpPosition.second] = (preDelay % 128).toByte()

        dump[EPlate.LOW_CUT.dumpPosition.first] = (lowCut / 128).toByte()
        dump[EPlate.LOW_CUT.dumpPosition.second] = (lowCut % 128).toByte()

        dump[EPlate.HIGH_CUT.dumpPosition.first] = (highCut / 128).toByte()
        dump[EPlate.HIGH_CUT.dumpPosition.second] = (highCut % 128).toByte()

        dump[EPlate.HIGH_RATIO.dumpPosition.first] = highRatio
        dump[EPlate.LOW_RATIO.dumpPosition.first] = lowRatio
        dump[EPlate.LEVEL.dumpPosition.first] = level

        return super.toDump(dump)
    }
}