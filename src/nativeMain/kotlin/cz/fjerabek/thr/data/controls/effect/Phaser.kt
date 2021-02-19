package cz.fjerabek.thr.data.controls.effect

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.effect.EEffect
import cz.fjerabek.thr.data.enums.effect.EEffectType
import cz.fjerabek.thr.data.enums.effect.EPhaser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Phaser")
class Phaser (
        override val status: EStatus,
        val speed : Byte,
        val manual : Byte,
        val depth : Byte,
        val feedback : Byte) : Effect(EEffectType.PHASER) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EEffect.STATUS.dumpPosition])!!,
            dump[EPhaser.SPEED.dumpPosition],
            dump[EPhaser.MANUAL.dumpPosition],
            dump[EPhaser.DEPTH.dumpPosition],
            dump[EPhaser.FEEDBACK.dumpPosition]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EPhaser.SPEED.dumpPosition] = speed
        dump[EPhaser.MANUAL.dumpPosition] = manual
        dump[EPhaser.DEPTH.dumpPosition] = depth
        dump[EPhaser.FEEDBACK.dumpPosition] = feedback
        return super.toDump(dump)
    }
}