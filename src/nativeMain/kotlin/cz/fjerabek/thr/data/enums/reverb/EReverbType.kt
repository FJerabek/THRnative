package cz.fjerabek.thr.data.enums.reverb

enum class EReverbType(val id : Byte) {
    HALL(0x00),
    ROOM(0x01),
    PLATE(0x02),
    SPRING(0x03);

    companion object {
        private val map = values().associateBy(EReverbType::id)

        fun fromId(id: Byte) = map[id]
    }

}