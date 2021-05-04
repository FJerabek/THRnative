package cz.fjerabek.thr.cli

import kotlin.reflect.KProperty

/**
 * Command line argument exception
 * @param paramName Name of parameter
 * @param paramShortName short name of parameter
 * @param msg exception message
 */
open class CliArgumentException(val paramShortName: Char?, val paramName: String, msg: String) : Exception(msg)

/**
 * Required argument is missing in cli arguments
 * @param paramName Name of parameter
 * @param paramShortName short name of parameter
 * @param msg exception message
 */
class RequiredArgumentNotFoundException(paramShortName: Char?, paramName: String, msg: String) :
    CliArgumentException(paramShortName, paramName, msg)

/**
 * Thrown when parameter does not have value
 * @param paramName Name of parameter
 * @param paramShortName short name of parameter
 * @param msg exception message
 */
class ArgumentMissingValueException(paramShortName: Char?, paramName: String, msg: String) :
    CliArgumentException(paramShortName, paramName, msg)

/**
 * Value delegate interface for value loading
 */
interface ValueDelegate<T> {
    /**
     * Value getter
     */
    operator fun getValue(thisRef: Nothing?, prop: KProperty<*>): T

    /**
     * Value setter
     */
    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: T)

    /**
     * Value inject if lazy loading
     */
    fun inject()
}

class LazyLambdaValueDelegate<T>(val delegate: () -> T) : ValueDelegate<T> {
    var value: T? = null
    override fun getValue(thisRef: Nothing?, prop: KProperty<*>): T {
        value = delegate()
        return value!!
    }

    override fun setValue(thisRef: Nothing?, property: KProperty<*>, value: T) {
        this.value = value
    }

    override fun inject() {
        value = delegate()
    }
}

/**
 * CLI argument representation
 * @param shortName Short name of argument
 * @param longName Long name of argument
 * @param description description of argument shown in help
 * @param delegate value delegate for value injection
 */
data class Argument(
    val shortName: Char?,
    val longName: String,
    val description: String,
    val required: Boolean,
    val delegate: ValueDelegate<*>
)

/**
 * Data type of commandline argument
 */
enum class ArgType {
    STRING, BOOLEAN
}

/**
 * Command line argument parser
 * @param args command line argumets
 */
class ArgParser(private val args: Array<String>) {

    private val arguments: MutableList<Argument> = mutableListOf()

    /**
     * Lazy injects value from commandline parameters. When value is needed \[[ArgType.STRING]\] exception
     * [ArgumentMissingValueException] is thrown when value is used or [inject] is called
     * @param argType data type of value
     * @param shortName short name of parameter ex. -h
     * @param description description of parameter shown in help
     * @param longName long name of parameter ex. --help
     * @param required if parameter is required. If true the parameter will throw [RequiredArgumentNotFoundException] if its not present
     */
    @Suppress("UNCHECKED_CAST") //No way to check without inlining the function
    fun <T> option(
        argType: ArgType,
        shortName: Char? = null,
        longName: String,
        description: String,
        required: Boolean = false,

        ): ValueDelegate<T> {
        val delegate = when (argType) {
            ArgType.STRING -> {
                LazyLambdaValueDelegate {
                    val index = getIndex(shortName, longName, required)

                    if (index == -1) {
                        return@LazyLambdaValueDelegate "" as T
                    }

                    if (args.lastIndex < index + 1 || args[index + 1].startsWith("-") || args[index + 1].startsWith("--")) {
                        throw ArgumentMissingValueException(shortName, longName, "Parameter does not have value")
                    }

                    return@LazyLambdaValueDelegate args[index + 1] as T
                }
            }
            ArgType.BOOLEAN -> {
                LazyLambdaValueDelegate {
                    val index = getIndex(shortName, longName, required)
                    (index != -1) as T
                }
            }
        }
        arguments.add(Argument(shortName, longName, description, required, delegate))

        return delegate
    }

    /**
     * Inject all values which are normally lazily injected
     */
    fun inject() {
        arguments.forEach {
            it.delegate.inject()
        }
    }

    /**
     * Gets index of parameter by name or short name throws [RequiredArgumentNotFoundException] if is required and not found
     * @param shortName short name of parameter
     * @param longName long name of parameter
     * @param required parameter is required
     * @return index of parameter name
     */
    private fun getIndex(shortName: Char?, longName: String, required: Boolean): Int {
        val index =
            if (shortName != null && args.contains("-$shortName")) args.indexOf("-$shortName") else args.indexOf("--$longName")
        if (index == -1 && required) {
            throw RequiredArgumentNotFoundException(shortName, longName, "Required parameter is missing")
        }
        return index
    }

    /**
     * Prints help into stdout
     * @param binaryName name of the binary shown in help
     * @param description program description
     */
    fun printHelp(binaryName: String, description: String) {
        val builder = StringBuilder("Usage ")
        builder.append(binaryName).append(" [OPTION]...")
        builder.appendLine()
        builder.appendLine(description)
        builder.appendLine("Options:")
        arguments.forEach {
            builder.append(if (it.shortName == null) "\t" else "-${it.shortName}\t")
            builder.append(
                "--${it.longName.padEnd(20, ' ')}"
            )
            builder.append(if (it.required) "required" else "".padEnd(8, ' '))
            builder.appendLine(it.description)
        }
        println(builder)
    }
}