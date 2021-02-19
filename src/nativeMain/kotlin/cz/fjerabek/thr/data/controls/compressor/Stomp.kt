package cz.fjerabek.thr.data.controls.compressor

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.compressor.ECompressor
import cz.fjerabek.thr.data.enums.compressor.ECompressorType
import cz.fjerabek.thr.data.enums.compressor.EStomp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Stomp")
class Stomp (
        override val status: EStatus,
        val sustain : Byte,
        val output : Byte, ):
        Compressor(ECompressorType.STOMP) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[ECompressor.STATUS.dumpPosition])!!,
            dump[EStomp.SUSTAIN.dumpPosition],
            dump[EStomp.OUTPUT.dumpPosition]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EStomp.SUSTAIN.dumpPosition] = sustain
        dump[EStomp.OUTPUT.dumpPosition] = output
        return super.toDump(dump)
    }
}