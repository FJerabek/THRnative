package cz.fjerabek.thr.cli

class UnknownCommandException(message: String) : Exception(message)

/**
 * Class representing commandline command
 */
abstract class CliCommand {
    /**
     * Command arguments
     */
    private var args: List<String> = listOf()

    /**
     * Sets command line command arguments
     * @param args arguments
     */
    fun setArgs(args: List<String>) {
        this.args = args
    }

    companion object {
        /**
         * Returns command instance from name. Without arguments set
         */
        fun fromName(name: String): CliCommand {
            return when (name) {
                "help", "h" -> {
                    HelpCommand()
                }
                "status" -> {
                    CurrentCommand()
                }

                "version" -> {
                    VersionCommand()
                }

                "shutdown" -> {
                    ShutdownCommand()
                }

                "activeIndex", "ai" -> {
                    GetActivePresetIndexCommand()
                }


                else -> {
                    throw UnknownCommandException("$name command is not known command name")
                }
            }
        }
    }
}

/**
 * Help command
 */
class HelpCommand : CliCommand()

/**
 * Command for displaying current draw
 */
class CurrentCommand : CliCommand()

/**
 * Command displaying current expansion board version
 */
class VersionCommand : CliCommand()

/**
 * Command for device shutdown
 */
class ShutdownCommand : CliCommand()

/**
 * Prints active preset index
 */
class GetActivePresetIndexCommand : CliCommand()

