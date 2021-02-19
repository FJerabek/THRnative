package cz.fjerabek.thr.data.enums.compressor

import cz.fjerabek.thr.data.enums.IControlProperty

enum class EStomp(val id : Byte, val  max : Byte, val min : Byte, val dumpPosition : Int) : IControlProperty {
    SUSTAIN(0x11, 0x64, 0x00, 162),
    OUTPUT(0x12, 0x64, 0x00, 163);

    override fun getPropertyId(): Byte {
        return this.id
    }

    override fun getMaximumValue(): Int {
        return this.max.toInt()
    }

    override fun getMinimumValue(): Int {
        return this.min.toInt()
    }
}