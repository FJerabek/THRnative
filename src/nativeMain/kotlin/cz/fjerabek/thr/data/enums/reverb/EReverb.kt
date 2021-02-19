package cz.fjerabek.thr.data.enums.reverb

import cz.fjerabek.thr.data.enums.IControlProperty

enum class EReverb (val id : Byte, val max : Byte, val min : Byte, val dumpPosition: Int) : IControlProperty {
    STATUS(0x4F, 0x7F, 0x00, 224),
    TYPE(0x40, 0x03, 0x00, 209);

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