package cz.fjerabek.thr.data.uart

import cz.fjerabek.thr.data.bluetooth.IBluetoothMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

abstract class UartMessage: IBluetoothMessage {
    companion object {
        const val CMD_FW = "\$fwv\r\n"
        const val CMD_STATUS = "\$sta\r\n"
        const val CMD_HBT = "\$hbt\r\n"
        const val CMD_SHUTDOWN = "\$off\r\n"
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