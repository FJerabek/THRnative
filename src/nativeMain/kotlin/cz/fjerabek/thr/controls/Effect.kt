package cz.fjerabek.thr.controls

import cz.fjerabek.thr.controls.effect.*
import cz.fjerabek.thr.enums.EStatus
import cz.fjerabek.thr.enums.effect.EEffect
import cz.fjerabek.thr.enums.effect.EEffectType
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
abstract class Effect(
    val effectType: EEffectType
) : IControl {

    abstract val status : EStatus

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EEffect.STATUS.dumpPosition] = status.value
        dump[EEffect.TYPE.dumpPosition] = effectType.id
        return dump
    }


    companion object {
        fun fromDump(dump : ByteArray) : Effect {

            return when(EEffectType.fromId(dump[EEffect.TYPE.dumpPosition])!!) {
                EEffectType.CHORUS -> Chorus(dump)
                EEffectType.FLANGER -> Flanger(dump)
                EEffectType.TREMOLO -> Tremolo(dump)
                EEffectType.PHASER -> Phaser(dump)
            }
        }
    }
}