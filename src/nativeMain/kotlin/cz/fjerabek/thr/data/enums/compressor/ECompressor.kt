package cz.fjerabek.thr.data.enums.compressor

import cz.fjerabek.thr.data.enums.IControlProperty

enum class ECompressor(val id : Byte, val max : Byte, val min : Byte, val dumpPosition : Int) : IControlProperty {
    STATUS(0x1F, 0x7F, 0x00, 176),
    TYPE(0x10, 0x01,0x00, 161);

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