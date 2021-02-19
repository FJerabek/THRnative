package cz.fjerabek.thr.data.controls.effect

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.effect.EChorus
import cz.fjerabek.thr.data.enums.effect.EEffect
import cz.fjerabek.thr.data.enums.effect.EEffectType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Chorus")
class Chorus(
        override val status: EStatus,
        val speed : Byte,
        val depth : Byte,
        val mix : Byte):
        Effect(EEffectType.CHORUS) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[EEffect.STATUS.dumpPosition])!!,
            dump[EChorus.SPEED.dumpPosition],
            dump[EChorus.DEPTH.dumpPosition],
            dump[EChorus.MIX.dumpPosition]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EChorus.SPEED.dumpPosition] = speed
        dump[EChorus.DEPTH.dumpPosition] = depth
        dump[EChorus.MIX.dumpPosition] = mix
        return super.toDump(dump)
    }
}