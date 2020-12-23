package cz.fjerabek.thr.bluetooth

import bluez.BTPROTO_RFCOMM
import bluez.register_rfcomm_sdp
import bluez.sockaddr_rc
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import kotlinx.cinterop.*
import platform.posix.*

class BluetoothConnectionClosedException(message: String) : Exception(message)
class BluetoothUnknownMessageException(message: String) : Exception(message)

@ExperimentalUnsignedTypes
object Bluetooth {
    const val SERVICE_NAME = "THR Controller"
    const val SERVICE_DESC = "THR controller communication service"
    const val SERVICE_PROV = "FJerabek"
    const val BLUETOOTH_CHANNEL: uint8_t = 11u

    private val localAddr = cValue<sockaddr_rc> {
        rc_family = AF_BLUETOOTH.toUShort()
        rc_channel = BLUETOOTH_CHANNEL
    }
    private val socket: Int

    init {
        checkBluetoothPermissions()

        socket = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)

        memScoped {
            bind(socket, localAddr.ptr.reinterpret(), sizeOf<sockaddr_rc>().toUInt())
            listen(socket, 1)
        }

        localAddr.useContents {
            register_rfcomm_sdp(
                rc_channel,
                SERVICE_NAME.cstr,
                SERVICE_DESC.cstr,
                SERVICE_PROV.cstr
            )
        }
    }

    fun acceptConnection(): BluetoothConnection {
        memScoped {
            val opt = cValue<socklen_tVar>()
            val remoteAddr = cValue<sockaddr_rc>()

            debug { "Bluetooth accepting connection" }
            val client = accept(socket, remoteAddr.ptr.reinterpret(), opt.ptr)

            return BluetoothConnection(client)
        }
    }

    fun checkBluetoothPermissions() {
        if( access( "/var/run/sdp", F_OK ) != 0 ) {
            error {
                "Bluetooth sdp not found"
            }
            exit(1)
        }

        if( access( "/var/run/sdp", R_OK) != 0 ) {
            error {
                "No read access to bluetooth sdp"
            }
            exit(1)
        }

        if( access( "/var/run/sdp", R_OK) != 0 ) {
            error {
                "No read access to bluetooth sdp"
            }
            exit(1)
        }
    }
}