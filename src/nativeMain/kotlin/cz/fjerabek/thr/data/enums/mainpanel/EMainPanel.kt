package cz.fjerabek.thr.data.enums.mainpanel

import cz.fjerabek.thr.data.enums.IControlProperty

enum class EMainPanel (val id : Byte, val max : Byte, val min : Byte, val dumpPosition : Int) : IControlProperty {
    AMP(0x00, 0x07, 0x00, 145),
    GAIN(0x01, 0x64, 0x00, 146),
    MASTER(0x02, 0x64, 0x00, 147),
    BASS(0x03, 0x64, 0x00, 148),
    MIDDLE(0x04, 0x64, 0x00, 149),
    TREBLE(0x05, 0x64, 0x00, 150),
    CABINET(0x06, 0x05, 0x00, 151);

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