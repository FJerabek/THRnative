package cz.fjerabek.thr.data.enums.effect

import cz.fjerabek.thr.data.enums.IControlProperty

enum class ETremolo(val id : Byte,val max : Byte,val min : Byte, val dumpPosition : Int) : IControlProperty {
    FREQ(0x21, 0x64, 0x00, 178),
    DEPTH(0x22, 0x64, 0x00, 179);

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