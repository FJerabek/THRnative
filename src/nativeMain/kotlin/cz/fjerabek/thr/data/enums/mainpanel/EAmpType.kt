package cz.fjerabek.thr.data.enums.mainpanel

enum class EAmpType (val id : Byte) {
    CLEAN(0x00),
    CRUNCH(0x01),
    LEAD(0x02),
    BRIT_HI(0x03),
    MODERN(0x04),
    BASS(0x05),
    ACO(0x06),
    FLAT(0x07);

    companion object {
        private val map = values().associateBy(EAmpType::id)

        fun fromId(id: Byte) = map[id]
    }

}