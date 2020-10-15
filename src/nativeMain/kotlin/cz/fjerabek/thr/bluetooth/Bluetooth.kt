package cz.fjerabek.thr.bluetooth

import bluez.BTPROTO_RFCOMM
import bluez.register_rfcomm_sdp
import bluez.sockaddr_rc
import kotlinx.cinterop.*
import platform.posix.*

@ExperimentalUnsignedTypes
object Bluetooth {
    const val SERVICE_NAME = "THR Controller"
    const val SERVICE_DESC = "THR controller communication service"
    const val SERVICE_PROV = "FJerabek"

    fun acceptConnection(): BluetoothConnection {
        memScoped {
            val opt = cValue<socklen_tVar>()
            val remoteAddr = cValue<sockaddr_rc>()
            val localAddr = cValue<sockaddr_rc> {
                rc_family = AF_BLUETOOTH.toUShort()
                rc_channel = 11u
            }

            val s = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)

            bind(s, localAddr.ptr.reinterpret(), sizeOf<sockaddr_rc>().toUInt())
            listen(s, 1)

            localAddr.useContents {
                register_rfcomm_sdp(rc_channel,
                        SERVICE_NAME.cstr,
                        SERVICE_DESC.cstr,
                        SERVICE_PROV.cstr
                )
            }


            println("Waiting for connection")
            val client = accept(s, remoteAddr.ptr.reinterpret(), opt.ptr)
            println("Connected")
            return BluetoothConnection(client)
        }
    }
}