package cz.fjerabek.thr.data.controls.compressor

import cz.fjerabek.thr.data.controls.IControl
import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.compressor.ECompressor
import cz.fjerabek.thr.data.enums.compressor.ECompressorType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
abstract class Compressor(
    //Default value is useless since it is specified by inheriting class
    @Transient
    val compressorType: ECompressorType = ECompressorType.STOMP
) : IControl {

    abstract val status : EStatus


    override fun toDump(dump: ByteArray): ByteArray {
        dump[ECompressor.STATUS.dumpPosition] = status.value
        dump[ECompressor.TYPE.dumpPosition] = compressorType.id

        return dump
    }

    companion object {
        fun fromDump(dump : ByteArray) : Compressor {
            return when(ECompressorType.fromId(dump[ECompressor.TYPE.dumpPosition])!!){
                ECompressorType.STOMP -> Stomp(dump)
                ECompressorType.RACK -> Rack(dump)
            }
        }
    }

}