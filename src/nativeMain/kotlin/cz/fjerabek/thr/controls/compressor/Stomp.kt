package cz.fjerabek.thr.controls.compressor

import cz.fjerabek.thr.controls.Compressor
import cz.fjerabek.thr.enums.EStatus
import cz.fjerabek.thr.enums.compressor.ECompressor
import cz.fjerabek.thr.enums.compressor.ECompressorType
import cz.fjerabek.thr.enums.compressor.EStomp
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