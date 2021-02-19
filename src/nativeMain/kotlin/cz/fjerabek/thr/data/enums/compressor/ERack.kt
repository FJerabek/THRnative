package cz.fjerabek.thr.data.enums.compressor

import cz.fjerabek.thr.data.enums.IControlProperty

enum class ERack(val id : Byte, val max : Int, val min : Byte, val dumpPosition: Pair<Int, Int>) : IControlProperty {
    THRESHOLD(0x11, 0x258, 0x00, Pair(162, 163)),
    ATTACK(0x13, 0x64, 0x00, Pair(164, -1)),
    RELEASE(0x14, 0x64, 0x00, Pair(165, -1)),
    RATIO(0x15, 0x05, 0x00, Pair(166, -1)),
    KNEE(0x16, 0x02, 0x00, Pair(167, -1)),
    OUTPUT(0x17, 0x258, 0x00, Pair(168, 169));

    override fun getPropertyId(): Byte {
        return this.id
    }

    override fun getMaximumValue(): Int {
        return this.max
    }

    override fun getMinimumValue(): Int {
        return this.min.toInt()
    }
}