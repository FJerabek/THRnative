package cz.fjerabek.thr


private const val ANSI_ESCAPE = "\u001b["
private const val ANSI_RESET = "\u001b[0m"

enum class ANSIColor(val ansi: Int) {
    RED(31),
    GREEN(32),
    WHITE(37),
    YELLOW(33)
}

fun String.color(color: ANSIColor) = "${ANSI_ESCAPE}${color.ansi}m${this}${ANSI_RESET}"