package cz.fjerabek.thr.data.enums.effect

enum class EEffectType(val id : Byte) {
    CHORUS(0x00),
    FLANGER(0x01),
    TREMOLO(0x02),
    PHASER(0x03);

    companion object {
        private val map = values().associateBy(EEffectType::id)

        fun fromId(id: Byte) = map[id]
    }
}