package cz.fjerabek.thr.data.enums.reverb

import cz.fjerabek.thr.data.enums.IControlProperty

enum class ESpring(val id : Byte, val max : Byte, val min : Byte, val dumpPosition : Int) : IControlProperty {
    REVERB(0x41, 0x64, 0x00, 210),
    FILTER(0x42, 0x64, 0x00, 211);

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