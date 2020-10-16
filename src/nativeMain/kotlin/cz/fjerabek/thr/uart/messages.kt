package cz.fjerabek.thr.uart

abstract class UartMessage {
    companion object {
        fun fromString(string: String): UartMessage {
            val params = string.split(";")

            //TODO: Some better way to do this
            when(params[0]) {
                "\$btn" -> {
                    return ButtonMessage(
                        params[1].toInt(),
                        params[2].trim().toInt() == 1,
                        if (params.size == 4) params[3].trim().toLong() else 0L
                    )
                }
                "\$ok" -> {
                    when (params.size) {
                        1 -> {
                            return ShutdownMessage(true);
                        }
                        4 -> {
                            return FWVersionMessage(
                                params[1].trim().toInt(),
                                params[2].trim().toInt(),
                                params[3].trim().toInt()
                            )
                        }
                        else -> {
                            return StatusMessage(
                                params[1].trim().toLong(),
                                params[2].trim().toInt(),
                                ECharging.fromValue(params[3])!!,
                                params[4].trim().toInt()
                            )
                        }
                    }
                }
                else -> {
                    throw UnsupportedOperationException("Received invalid message")
                }
            }
        }
    }
}

data class ButtonMessage(
    var id : Int,
    var pressed : Boolean,
    var pressedTime : Long
): UartMessage()

data class FWVersionMessage(
    var major : Int,
    var minor : Int,
    var patch : Int
): UartMessage()

data class StatusMessage(
    var uptime : Long,
    var battery : Int,
    var charging : ECharging,
    var current : Int
) : UartMessage()

data class ShutdownMessage(
    val ok: Boolean
) : UartMessage()
