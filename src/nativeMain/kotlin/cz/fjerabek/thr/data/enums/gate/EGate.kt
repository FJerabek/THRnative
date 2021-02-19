package cz.fjerabek.thr.data.enums.gate

import cz.fjerabek.thr.data.enums.IControlProperty

enum class EGate(val id : Byte,val max : Byte,val min : Byte, val dumpPosition : Int) : IControlProperty {
    STATUS (0x5F, 0x7F, 0x00, 240),
    THRESHOLD(0x51, 0x64, 0x00, 226),
    RELEASE(0x52, 0x64, 0x00, 227);

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