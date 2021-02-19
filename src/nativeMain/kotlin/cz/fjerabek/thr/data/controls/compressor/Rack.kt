package cz.fjerabek.thr.data.controls.compressor

import cz.fjerabek.thr.data.enums.EStatus
import cz.fjerabek.thr.data.enums.compressor.ECompressor
import cz.fjerabek.thr.data.enums.compressor.ECompressorType
import cz.fjerabek.thr.data.enums.compressor.ERack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Rack")
class Rack(
        override val status: EStatus,
        val threshold : Int,
        val attack: Byte,
        val release : Byte,
        val ratio : Byte,
        val knee : Byte,
        val output : Int
): Compressor(ECompressorType.RACK) {

    constructor(dump: ByteArray): this(
            EStatus.fromValue(dump[ECompressor.STATUS.dumpPosition])!!,
            (dump[ERack.THRESHOLD.dumpPosition.first] * 128) + dump[ERack.THRESHOLD.dumpPosition.second],
            dump[ERack.ATTACK.dumpPosition.first],
            dump[ERack.RELEASE.dumpPosition.first],
            dump[ERack.RATIO.dumpPosition.first],
            dump[ERack.KNEE.dumpPosition.first],
            (dump[ERack.OUTPUT.dumpPosition.first] * 128) + dump[ERack.OUTPUT.dumpPosition.second]
    )

    override fun toDump(dump: ByteArray): ByteArray {
        dump[ERack.THRESHOLD.dumpPosition.first] = (threshold / 128).toByte()
        dump[ERack.THRESHOLD.dumpPosition.second] = (threshold % 128).toByte()

        dump[ERack.ATTACK.dumpPosition.first] = attack
        dump[ERack.RELEASE.dumpPosition.first] = release
        dump[ERack.RATIO.dumpPosition.first] = ratio

        dump[ERack.KNEE.dumpPosition.first] = knee
        dump[ERack.OUTPUT.dumpPosition.first] = (output / 128).toByte()
        dump[ERack.OUTPUT.dumpPosition.second] = (output % 128).toByte()

        return super.toDump(dump)
    }
}