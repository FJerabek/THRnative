package cz.fjerabek.thr.data.enums.compressor

enum class ECompressorType (val id : Byte) {
    STOMP(0x00),
    RACK(0x01);

    companion object {
        private val map = values().associateBy(ECompressorType::id)

        fun fromId(id: Byte) = map[id]
    }
}