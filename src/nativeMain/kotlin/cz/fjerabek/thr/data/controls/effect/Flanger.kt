package cz.fjerabek.thr.data.controls.effect

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.effect.EEffect
import cz.fjerabek.thr.data.enums.effect.EEffectType
import cz.fjerabek.thr.data.enums.effect.EFlanger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Flanger")
class Flanger(
        override val status: EStatus,
        val speed : Byte,
        val manual : Byte,
        val depth : Byte,
        val feedback : Byte,
        val spread : Byte):
        Effect(EEffectType.FLANGER) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EEffect.STATUS.dumpPosition])!!,
            dump[EFlanger.SPEED.dumpPosition],
            dump[EFlanger.MANUAL.dumpPosition],
            dump[EFlanger.DEPTH.dumpPosition],
            dump[EFlanger.FEEDBACK.dumpPosition],
            dump[EFlanger.SPREAD.dumpPosition]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EFlanger.SPEED.dumpPosition] = speed
        dump[EFlanger.MANUAL.dumpPosition] = manual
        dump[EFlanger.DEPTH.dumpPosition] = depth
        dump[EFlanger.FEEDBACK.dumpPosition] = feedback
        dump[EFlanger.SPREAD.dumpPosition] = spread
        return super.toDump(dump)
    }
}