package cz.fjerabek.thr


private const val ANSI_ESCAPE = "\u001b["
private const val ANSI_RESET = "\u001b[0m"

/**
 * Color codes in command line
 * @param ansi ansi code
 */
enum class ANSIColor(val ansi: Int) {
    /**
     * Red color
     */
    RED(31),

    /**
     * Green color
     */
    GREEN(32),

    /**
     * White color
     */
    WHITE(37),

    /**
     * Yellow color
     */
    YELLOW(33)
}

/**
 * Colorizes string with provided color
 * @param color ansi color
 * @return colorized string
 */
fun String.color(color: ANSIColor) = "${ANSI_ESCAPE}${color.ansi}m${this}${ANSI_RESET}"