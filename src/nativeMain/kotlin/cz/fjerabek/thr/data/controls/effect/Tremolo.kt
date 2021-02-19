package cz.fjerabek.thr.data.controls.effect

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.effect.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Tremolo")
class Tremolo (
    override val status: EStatus,
    val freq : Byte,
    val depth : Byte
) : Effect(EEffectType.TREMOLO) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EEffect.STATUS.dumpPosition])!!,
            dump[ETremolo.FREQ.dumpPosition],
            dump[ETremolo.DEPTH.dumpPosition]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[ETremolo.FREQ.dumpPosition] = freq
        dump[ETremolo.DEPTH.dumpPosition] = depth
        return super.toDump(dump)
    }
}