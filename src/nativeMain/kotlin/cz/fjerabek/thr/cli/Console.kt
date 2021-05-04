package cz.fjerabek.thr.cli

import com.badoo.reaktive.observable.observable
import cz.fjerabek.thr.ANSIColor
import cz.fjerabek.thr.color

/**
 * Object representing CLI console
 */
object Console {
    /**
     * Prints shell prefix to stdout
     */
    private fun printShell() {
        print("THR-Comm".color(ANSIColor.GREEN) + " > ".color(ANSIColor.RED))

    }

    /**
     * Reads CLI command from stdin
     * @return read cli command
     */
    private fun readCommand(): CliCommand? {
        printShell()
        val line = readLine()

        if(line.isNullOrEmpty()) return null
        return fromString(line)
    }

    /**
     * Converts cli command to object from string representation
     * @param string string representation
     * @return command instance
     */
    private fun fromString(string: String) : CliCommand {
        val commandParts = string.split(" ")
        val command = CliCommand.fromName(commandParts[0])
        if(commandParts.size > 1) {
            command.setArgs(commandParts.subList(1,commandParts.lastIndex))
        }
        return command
    }

    /**
     * Starts cli console
     * @return returns observable called when command is read from CLI
     */
    fun runConsole()= observable<CliCommand> {
        while(true) {
            try{
                readCommand()?.let { command ->
                    it.onNext(command)
                }
            } catch(e: UnknownCommandException) {
                println(e.message?.color(ANSIColor.RED))
            }
        }
    }
}