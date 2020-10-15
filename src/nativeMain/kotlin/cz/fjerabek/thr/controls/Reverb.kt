package cz.fjerabek.thr.controls

import cz.fjerabek.thr.controls.reverb.*
import cz.fjerabek.thr.enums.EStatus
import cz.fjerabek.thr.enums.reverb.EReverb
import cz.fjerabek.thr.enums.reverb.EReverbType
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Reverb")
abstract class Reverb(
    val reverbType: EReverbType
) : IControl {

    abstract val status : EStatus

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EReverb.STATUS.dumpPosition] = status.value
        dump[EReverb.TYPE.dumpPosition] = reverbType.id
        return dump
    }

    companion object {
        fun fromDump(dump : ByteArray) : Reverb {

            return when(EReverbType.fromId(dump[EReverb.TYPE.dumpPosition])!!) {
                EReverbType.HALL -> Hall(dump)
                EReverbType.ROOM -> Room(dump)
                EReverbType.PLATE -> Plate(dump)
                EReverbType.SPRING -> Spring(dump)
            }
        }
    }
}