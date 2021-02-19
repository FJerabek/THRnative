package cz.fjerabek.thr.data.enums

enum class EStatus (val value : Byte){
    ON(0x00),
    OFF(0x7F);

    companion object {
        private val map = values().associateBy(EStatus::value)

        fun fromValue(value: Byte) = map[value]
    }
}