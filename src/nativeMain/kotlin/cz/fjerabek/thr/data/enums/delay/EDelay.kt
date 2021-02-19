package cz.fjerabek.thr.data.enums.delay

import cz.fjerabek.thr.data.enums.IControlProperty

enum class EDelay(val id : Byte, val max : Short, val min : Short, val dumpPosition : Pair<Int, Int>) :
    IControlProperty {
    STATUS(0x3F, 0x7F, 0x00, Pair(208, -1)),
    TIME(0x31, 0x270f, 0x01, Pair(194, 195)),
    FEEDBACK(0x33, 0x64, 0x00, Pair(196, -1)),
    HIGH_CUT(0x34, 0x3E81, 0x3E8, Pair(197, 198)),
    LOW_CUT(0x36, 0x1F40, 0x15, Pair(199, 200)),
    LEVEL(0x38, 0x64, 0x00, Pair(201, -1));

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