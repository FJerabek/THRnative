package cz.fjerabek.thr.data.midi

import cz.fjerabek.thr.data.controls.*
import cz.fjerabek.thr.data.controls.compressor.Compressor
import cz.fjerabek.thr.data.controls.effect.Effect
import cz.fjerabek.thr.data.controls.reverb.Reverb
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Dump")
class PresetMessage(
    var name : String,
    var mainPanel: MainPanel,
    var compressor: Compressor? = null,
    var effect: Effect? = null,
    var delay: Delay? = null,
    var reverb: Reverb? = null,
    var gate: Gate? = null): IMidiMessage {

    constructor(dump: ByteArray): this(
            dump.sliceArray(prefix.size..80)
                    .dropLastWhile{ it.toInt() == 0x0}
                    .toByteArray()
                    .decodeToString(),
            MainPanel.fromDump(dump),
            Compressor.fromDump(dump),
            Effect.fromDump(dump),
            Delay.fromDump(dump),
            Reverb.fromDump(dump),
            Gate.fromDump(dump)
    )

    private fun calculateChecksum(data : ByteArray) : Byte {
        var count = 0x71
        for (i in data) {
            count += i
        }
        return ((count.inv() + 1) and 0x7f).toByte()
    }

    companion object {
        val prefix = byteArrayOf(67, 125, 0, 2, 12, 68, 84, 65, 49, 65, 108, 108, 80, 0, 0, 127, 127)
    }

    override val sysex: ByteArray
        get() {
            val dumpArray = prefix + ByteArray(257) {0}
            var nameDump = name.encodeToByteArray()
            if(nameDump.size > 64) {
                nameDump = nameDump.sliceArray(0..63)
            }

            nameDump.copyInto(dumpArray, prefix.size)
            mainPanel.toDump(dumpArray)
            compressor?.toDump(dumpArray)
            effect?.toDump(dumpArray)
            delay?.toDump(dumpArray)
            reverb?.toDump(dumpArray)
            gate?.toDump(dumpArray)

            dumpArray[dumpArray.lastIndex] = calculateChecksum(dumpArray.sliceArray(prefix.size .. dumpArray.lastIndex))
            return dumpArray
        }

}