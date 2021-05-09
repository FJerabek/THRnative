@file:Suppress("UNCHECKED_CAST", "EXPERIMENTAL_UNSIGNED_TYPES")

package cz.fjerabek.thr

import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.observable.subscribe
import com.badoo.reaktive.observable.subscribeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.badoo.reaktive.scheduler.mainScheduler
import com.badoo.reaktive.utils.atomic.AtomicBoolean
import com.badoo.reaktive.utils.atomic.AtomicLong
import com.badoo.reaktive.utils.atomic.AtomicReference
import com.badoo.reaktive.utils.atomic.update
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.LogUtils.info
import cz.fjerabek.thr.LogUtils.warn
import cz.fjerabek.thr.bluetooth.*
import cz.fjerabek.thr.cli.*
import cz.fjerabek.thr.data.bluetooth.*
import cz.fjerabek.thr.file.PresetsManager
import cz.fjerabek.thr.midi.Midi
import cz.fjerabek.thr.midi.MidiDisconnectedException
import cz.fjerabek.thr.data.midi.ChangeMessage
import cz.fjerabek.thr.data.midi.HeartBeatMessage
import cz.fjerabek.thr.data.midi.IMidiMessage
import cz.fjerabek.thr.data.midi.PresetMessage
import cz.fjerabek.thr.data.uart.*
import cz.fjerabek.thr.uart.*
import glib.*
import kotlinx.cinterop.*
import kotlinx.serialization.ExperimentalSerializationApi
import platform.posix.*
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.SharedImmutable

/**
 * Midi device
 */
@SharedImmutable
private val midi = AtomicReference<Midi?>(null)

/**
 * Connected bluetooth device
 */
@ExperimentalUnsignedTypes
@SharedImmutable
private val bluetoothConnection: AtomicReference<BluetoothConnection?> = AtomicReference(null)

/**
 * Loaded presets
 */
@SharedImmutable
private val presets = AtomicReference<List<PresetMessage>>(listOf())

/**
 * Midi port file location
 */
@SharedImmutable
private val midiPort = AtomicReference("/dev/midi3")

/**
 * Preset save file location
 */
@SharedImmutable
private val presetsFilePath = AtomicReference("presets.json")

/**
 * Current index of selected preset -1 is not saved custom
 */

@SharedImmutable
private val currentPreset = AtomicInt(-1)

/**
 * Id of left button timeout task
 */
@SharedImmutable
private val leftButtonTimeoutId = AtomicLong(-1)

/**
 * Id of right button timeout task
 */
@SharedImmutable
private val rightButtonTimeoutId = AtomicLong(-1)

/**
 * Specifies that next UART request is from CLI
 */
@SharedImmutable
private val isCliRq = AtomicBoolean(false)

/**
 * Main loop reference
 */
private val mainLoop = g_main_loop_new(null, 0)

/**
 * Default value for preset file argument
 */
const val PRESETS_DEFAULT = "presets.json"

/**
 * Called on midi message receive
 * @param message received message
 */
@ExperimentalUnsignedTypes
fun onMidiMessage(message: IMidiMessage) {
    if (message is HeartBeatMessage) return
    if (message is ChangeMessage && currentPreset.value != -1) {
        currentPreset.value = -1
        bluetoothConnection.value?.run {
            sendMessage(PresetSelect(-1))
        }
    }
    bluetoothConnection.value?.run {
        try {
            sendMessage(message)
        } catch (e: BluetoothConnectionClosedException) {
            bluetoothError(e)
        }
    }
}

/**
 * Called on midi read/write error
 * @param throwable thrown exception
 */
@ExperimentalUnsignedTypes
fun onMidiError(throwable: Throwable) {
    when (throwable) {
        is MidiDisconnectedException -> {
            "MIDI Disconnected".info()
            midi.value?.close()
            bluetoothConnection.value?.sendMessage(Connected(false))
            midiConnect(midiPort.value)
            midi.value = null
        }
        else -> "Midi error ${throwable.stackTraceToString()}".error()
    }
}

/**
 * Starts connecting to THR midi device.
 * @param port path to /dev file of midi device
 */
@ExperimentalUnsignedTypes
fun midiConnect(port: String) {
    observable<Midi> {
        it.onNext(Midi.waitForConnection(port))
    }.observeOn(ioScheduler)
        .subscribeOn(mainScheduler)
        .subscribe {
            midi.value = it
            bluetoothConnection.value?.sendMessage(Connected(true))
            it.requestDump()
            it.startMessageReceiver()
                .subscribeOn(mainScheduler)
                .subscribe(onError = ::onMidiError, onNext = ::onMidiMessage)
        }
}

/**
 * Called on bluetooth read/write error
 * @param e exception
 */
@ExperimentalUnsignedTypes
fun bluetoothError(e: Throwable) {
    when (e) {
        is BluetoothConnectionClosedException -> {
            "Bluetooth connection closed".info()
            bluetoothConnection.value = null
        }
        else -> "Bluetooth error: ${e.stackTraceToString()}".error()
    }
}

/**
 * Called when bluetooth message received
 * @param message received message
 */
@ExperimentalUnsignedTypes
fun bluetoothMessage(message: IBluetoothMessage) {
    when (message) {
        is IMidiMessage -> midi.value?.sendMessage(message)
        is FwVersionRq -> Uart.requestFirmware()
        is HwStatusRq -> Uart.requestStatus()
        is PresetsRq -> bluetoothConnection.value?.sendMessage(PresetsResponse(presets.value))
        is CurrentPresetRq -> midi.value?.requestDump()
        is CurrentPresetIndexRq -> bluetoothConnection.value?.sendMessage(PresetSelect(currentPreset.value))
        is Lamp -> midi.value?.lamp(message.on)
        is WideStereo -> midi.value?.wideStereo(message.on)
        is ConnectedRq -> bluetoothConnection.value?.sendMessage(Connected(midi.value != null))
        is RemovePresetRq -> {
            if (message.index >= presets.value.size) {
                "Invalid preset index: ${message.index} preset size: ${presets.value.size}".error()
            } else {
                presets.update {
                    it.filterIndexed { index, _ -> index != message.index }
                }
                PresetsManager.savePresets(presetsFilePath.value, presets.value)
            }
        }
        is AddPresetRq -> {
            presets.update {
                it.toMutableList().run {
                    add(message.preset)
                    this
                }
            }
            PresetsManager.savePresets(presetsFilePath.value, presets.value)
        }
        is SetPresetsRq -> {
            presets.value = message.presets.toMutableList()
            PresetsManager.savePresets(presetsFilePath.value, presets.value)
        }
        is PresetSelect -> {
            when {
                message.index == -1 -> {
                    currentPreset.value = -1
                }
                message.index >= presets.value.size -> {
                    "Invalid preset index: ${message.index} preset size: ${presets.value.size}".error()
                }
                else -> {
                    midi.value?.sendMessage(presets.value[message.index])
                    currentPreset.value = message.index
                }
            }
        }
    }
}

/**
 * Called when bluetooth connection is accepted
 * @param connection accepted connection
 */
@ExperimentalUnsignedTypes
fun bluetoothConnection(connection: BluetoothConnection) {
    "Bluetooth connected".info()
    bluetoothConnection.value = connection
    connection.startReceiver()
        .subscribeOn(ioScheduler)
        .subscribe(onError = ::bluetoothError, onNext = ::bluetoothMessage)
}

/**
 * Called on uart message received
 * @param message received message
 */
@ExperimentalUnsignedTypes
fun uartMessageReceived(message: UartMessage) {
    when (message) {
        is ButtonMessage -> {
            if (message.pressed) { //Run on press not release
                when (message.id) {
                    1 -> { //Left button
                        val leftButtonTimeout = staticCFunction<gpointer?, gboolean> {
                            leftButtonTimeoutId.value = -1
                            0 //Return false so the timeout is not scheduled anymore
                        }
                        leftButtonTimeoutId.value = g_timeout_add(3000, leftButtonTimeout, null).toLong()

                        if (currentPreset.value == -1 || currentPreset.value <= 0) {
                            currentPreset.value = presets.value.size - 1
                        } else {
                            currentPreset.value--
                        }
                    }
                    2 -> { //Right button
                        val rightButtonTimeout = staticCFunction<gpointer?, gboolean> {

                            "Setting Bluetooth to discoverable and pairable".info()
                            BluetoothAdapter.discoverable = true
                            BluetoothAdapter.pairable = true
                            rightButtonTimeoutId.value = -1
                            0 //Return false so the timeout is not scheduled anymore
                        }
                        rightButtonTimeoutId.value = g_timeout_add(3000, rightButtonTimeout, null).toLong()

                        if (currentPreset.value == -1 || currentPreset.value + 1 >= presets.value.size) {
                            currentPreset.value = 0
                        } else {
                            currentPreset.value++
                        }
                    }
                }
                try {
                    if(!presets.value.indices.contains(currentPreset.value)) return
                    midi.value?.sendMessage(presets.value[currentPreset.value])
                    bluetoothConnection.value?.sendMessage(PresetSelect(currentPreset.value))
                } catch (e: MidiDisconnectedException) {
                    onMidiError(e)
                }
            } else {
                when (message.id) {
                    1 -> { //Left button
                        if (leftButtonTimeoutId.value != -1L)
                            g_source_remove(leftButtonTimeoutId.value.toUInt())
                    }
                    2 -> { //Right button
                        if (rightButtonTimeoutId.value != -1L)
                            g_source_remove(rightButtonTimeoutId.value.toUInt())
                    }
                }
            }
        }
        is FWVersionMessage -> {
            if (isCliRq.value) {
                isCliRq.value = false
                println(message)
            } else {
                bluetoothConnection.value?.sendMessage(message)
            }
        }
        is StatusMessage -> {
            if (isCliRq.value) {
                isCliRq.value = false
                println(message)
            } else {
                bluetoothConnection.value?.sendMessage(message)
            }
        }

        is ShutdownMessage -> {
            "Shutting down".warn()
            system("shutdown -P now")
        }
        is HbtMessage -> {
        }
    }
}

/**
 * Sets up uart receiver
 */
@ExperimentalUnsignedTypes
fun setupUartReceiver() {
    Uart.startReceiver()
        .subscribeOn(ioScheduler)
        .subscribe(onNext = ::uartMessageReceived, onError = {
            when (it) {
                is UartReadException -> "Unable to read from uart".error()
                is UartWriteException -> "Unable to write to uart".error()
                else -> "Uart error: ${it.stackTraceToString()}".error()
            }
        })
}

/**
 * Processes cli command
 */
@ExperimentalUnsignedTypes
fun cliCommandProcessor(command: CliCommand) {
    try {
        when (command) {
            is HelpCommand -> {
                println(
                    "Command line interface is useful for getting basic information from THR-comm. Commands are: \n" +
                            "help, h\t\tPrints this text \n" +
                            "status\t\tPrints system status (uptime, battery %, charging state, current in mA) \n" +
                            "version\t\tPrints current FW version \n" +
                            "shutdown\tShuts down system \n" +
                            "activeIndex, ai\tPrints active preset index"
                )
            }
            is CurrentCommand -> {
                isCliRq.value = true
                Uart.requestStatus()
            }
            is VersionCommand -> {
                isCliRq.value = true
                Uart.requestFirmware()
            }
            is ShutdownCommand -> {
                Uart.requestShutdown()
            }
            is GetActivePresetIndexCommand -> {
                println(currentPreset.value)
            }
        }
    } catch (e: Exception) {
        "Exception processing command: $e".error()
    }
}


@ExperimentalSerializationApi
@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val argParser = ArgParser(args)
    val help: Boolean by argParser.option(ArgType.BOOLEAN, 'h', "help", "shows this text")
    val midiPortFile: String by argParser.option(ArgType.STRING, 'm', "midiFile", "dev file usually /dev/midi1", true)
    val disableUart: Boolean by argParser.option(ArgType.BOOLEAN, null, "disableUart", "disables UART communication")
    val disableHb: Boolean by argParser.option(ArgType.BOOLEAN, null, "disableHeartBeat", "disables sending HeartBeat")
    val enableConsole: Boolean by argParser.option(ArgType.BOOLEAN, 'c', "console", "enables development console")
    val presetsFile: String by argParser.option(
        ArgType.STRING,
        'p',
        "presetFile",
        "specifies presets file default is $PRESETS_DEFAULT"
    )
    val debug: Boolean by argParser.option(ArgType.BOOLEAN, 'd', "debug", "enables debug log output")

    if (help) {
        argParser.printHelp("THR-comm", "Communicate with THR 10 guitar combo and mobile configuration app")
        return
    }

    try {
        argParser.inject()
    } catch (e: Exception) {
        println("Invalid parameters try flag -h or --help for more info")
        exit(1)
    }

    if (debug) {
        LogUtils.logLevel.value = LogUtils.LogLevel.DEBUG
    }

    presetsFilePath.value = presetsFile.ifEmpty { PRESETS_DEFAULT }
    midiPort.value = midiPortFile

    """
        midiFile: $midiPortFile,
        disableUart: $disableUart,
        disableHb: $disableHb,
        enableConsole: $enableConsole,
        presetsFile: $presetsFile
    """.trimIndent().debug()

    signal(SIGINT, staticCFunction<Int, Unit> {

        g_main_loop_quit(mainLoop)
        g_main_loop_unref(mainLoop)
        Uart.close()
        exit(0)
    })

    PresetsManager.loadPresets(presetsFilePath.value)
        ?.let { loadedPresets ->
            loadedPresets.toMutableList().let {
                "Loaded ${loadedPresets.size} presets".info()
                presets.update {
                    loadedPresets
                }
            }
        } ?: run {
        "Creating preset save file".info()
        PresetsManager.savePresets(presetsFilePath.value, emptyList())
    }

    if (disableUart) {
        "UART receiver disabled".warn()
    } else {
        try {
            "Setting up UART receiver".info()
            setupUartReceiver()
        } catch (e: UartException) {
            "Error setting up UART ${e.message}".error()
            "Uart exception $e".debug()
            return
        }
    }

    "Starting MIDI connector".info()
    midiConnect(midiPort.value)

    "Registering Bluetooth SDP record".info()
    Bluetooth.sdpRegister() //Starts accepting connections
    "Registering Bluetooth agent".info()
    PinAgent.registerAgent()

    val heartbeat = staticCFunction<gpointer?, gboolean> {
        try {
            Uart.sendHeartBeat()
        } catch (e: Exception) {
            e.stackTraceToString().error()
        }
        1
    }

    if (disableHb || disableUart) {
        "Heartbeat sender is disabled".warn()
    } else {
        "Starting heartbeat sender".info()
        g_timeout_add(1000, heartbeat, null)
    }

    if (enableConsole) {
        "Console enabled".info()
        Console.runConsole()
            .subscribeOn(ioScheduler)
            .subscribe(onNext = ::cliCommandProcessor)
    }

    "Starting main loop".info()
    g_main_loop_run(mainLoop)
}
