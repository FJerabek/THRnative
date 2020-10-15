package cz.fjerabek.thr.controls

import cz.fjerabek.thr.controls.compressor.Rack
import cz.fjerabek.thr.controls.compressor.Stomp
import cz.fjerabek.thr.enums.EStatus
import cz.fjerabek.thr.enums.compressor.ECompressor
import cz.fjerabek.thr.enums.compressor.ECompressorType
import kotlinx.serialization.Serializable

@Serializable
abstract class Compressor(
    val compressorType: ECompressorType
) : IControl {

    abstract val status : EStatus


    override fun toDump(dump: ByteArray): ByteArray {
        dump[ECompressor.STATUS.dumpPosition] = status.value
        dump[ECompressor.TYPE.dumpPosition] = compressorType.id

        return dump
    }

    companion object {
        fun fromDump(dump : ByteArray) : Compressor {
            val status = EStatus.fromValue(dump[ECompressor.STATUS.dumpPosition])!!

            return when(ECompressorType.fromId(dump[ECompressor.TYPE.dumpPosition])!!){
                ECompressorType.STOMP -> Stomp(dump)
                ECompressorType.RACK -> Rack(dump)
            }
        }
    }

}