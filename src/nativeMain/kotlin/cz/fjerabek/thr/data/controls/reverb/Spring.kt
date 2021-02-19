package cz.fjerabek.thr.data.controls.reverb

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.reverb.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("Spring")
class Spring(
        override val status: EStatus,
        val reverb : Byte,
        val filter : Byte
) : Reverb(EReverbType.SPRING) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EReverb.STATUS.dumpPosition])!!,
            dump[ESpring.REVERB.dumpPosition],
            dump[ESpring.FILTER.dumpPosition]
    )
    override fun toDump(dump: ByteArray): ByteArray {
        dump[ESpring.REVERB.dumpPosition] = reverb
        dump[ESpring.FILTER.dumpPosition] = filter
        return super.toDump(dump)
    }
}