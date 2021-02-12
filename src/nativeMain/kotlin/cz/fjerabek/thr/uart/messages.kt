package cz.fjerabek.thr.uart

import com.badoo.reaktive.utils.atomic.getAndUpdate
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.bluetooth.IBluetoothMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

abstract class UartMessage: IBluetoothMessage {
    companion object {
        fun fromString(string: String): UartMessage {
            val params = string.split(";")

            when(params[0].trim()) {
                "\$btn" -> {
                    return ButtonMessage(
                        params[1].toInt(),
                        params[2].trim().toInt() == 1,
                        if (params.size == 4) params[3].trim().toLong() else 0L
                    )
                }
                "\$ok" -> {
                    var cmd: String = ""
                    uartQueue.getAndUpdate {
                        it.toMutableList().apply { cmd = removeLast() }
                    }
                    when (cmd) {
                        Uart.CMD_SHUTDOWN -> {
                            return ShutdownMessage(true);
                        }
                        Uart.CMD_FW -> {
                            return FWVersionMessage(
                                params[1].trim().toInt(),
                                params[2].trim().toInt(),
                                params[3].trim().toInt()
                            )
                        }
                        Uart.CMD_STATUS -> {
                            return StatusMessage(
                                params[1].trim().toLong(),
                                params[2].trim().toInt(),
                                ECharging.fromValue(params[3])!!,
                                params[4].trim().toInt()
                            )
                        }
                        Uart.CMD_HBT -> { return HbtMessage()}
                        else -> {
                            throw UnsupportedOperationException("Received invalid message")
                        }
                    }
                }
                else -> {
                    throw UnsupportedOperationException("Received invalid message: $params")
                }
            }
        }
    }
}

@Serializable
data class ButtonMessage(
    var id : Int,
    var pressed : Boolean,
    var pressedTime : Long
): UartMessage()

@Serializable
@SerialName("FwVersion")
data class FWVersionMessage(
    var major : Int,
    var minor : Int,
    var patch : Int
): UartMessage()

@Serializable
@SerialName("HwStatus")
data class StatusMessage(
    var uptime : Long,
    var battery : Int,
    var charging : ECharging,
    var current : Int
) : UartMessage()

@Serializable
data class ShutdownMessage(
    val ok: Boolean
) : UartMessage()

class HbtMessage : UartMessage()