package cz.fjerabek.thr.bluetooth

import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.bluetoothConnection
import cz.fjerabek.thr.glib.DBusUnknownMethodException
import cz.fjerabek.thr.glib.GLib
import glib.*
import kotlinx.cinterop.*

open class BluetoothException(message: String) : Exception(message)
class BluetoothConnectionClosedException(message: String) : BluetoothException(message)

@ExperimentalUnsignedTypes
/**
 * Bluetooth class implementing BlueZ DBus api
 * @param connectionCallback callback method. Called when connection is accepted
 */
object Bluetooth {
    const val BLUEZ_BUS_NAME = "org.bluez"
    private const val PROFILE_OBJECT_PATH = "/cz/fjerabek/thr"
    private const val SERIAL_PORT_PROFILE_UUID = "00001101-0000-1000-8000-00805f9b34fb"
    const val CUSTOM_UUID = "1FB0D116-1579-4CFF-BB97-51D22574BFA6"
    const val SERVICE_NAME = "THR Controller"
    const val SERVICE_DESC = "THR controller communication service"
    const val SERVICE_PROV = "FJerabek"

    private const val DBUS_PROFILE_OBJECT_DESCRIPTION =
        """
            <node>
                <interface name="org.bluez.Profile1">
                    <method name="Release" />
                    <method name="NewConnection">
                        <arg type="o" name="device" direction="in" />
                        <arg type="h" name="fd" direction="in" />
                        <arg type="a{sv}" name="fd_properties" direction="in" />
                    </method>
                    <method name="RequestDisconnection">
                        <arg type="o" name="device" direction="in" />
                    </method>
                </interface>
            </node>
        """


    private val connectionMethodCall = staticCFunction<
            CPointer<GDBusConnection>?,
            CPointer<gcharVar>?,
            CPointer<gcharVar>?,
            CPointer<gcharVar>?,
            CPointer<gcharVar>?,
            CPointer<GVariant>?,
            CPointer<GDBusMethodInvocation>?,
            gpointer?,
            Unit>
    { _/*DBus*/, sender, objectPath, interfaceName, methodName, parameters, invocation, _/*User Data*/ ->
        memScoped {
            val caller = allocPointerTo<gcharVar>()
            val fdIndex = alloc<gint32Var>()
            val properties = allocPointerTo<GVariantIter>()

            """
                Method call sender: ${sender?.toKString()} 
                objectPath: ${objectPath?.toKString()}
                interfaceName: ${interfaceName?.toKString()}
                methodName: ${methodName?.toKString()}
            """.trimIndent().debug()

            when (methodName?.toKString()) {
                "NewConnection" -> {
                    g_variant_get(parameters, "(oha{sv})", caller.ptr, fdIndex, properties.ptr)
                    val fdList = g_dbus_message_get_unix_fd_list(g_dbus_method_invocation_get_message(invocation))
                    val fd = g_unix_fd_list_get(fdList, fdIndex.value, null)
                    "Bluetooth file descriptor: $fd".debug()
                    bluetoothConnection(BluetoothConnection(fd))
                }
                "Release" -> {
                    "Bluetooth profile released".error()
                }
                else -> {
                    methodName?.let {
                        throw DBusUnknownMethodException("Received call to method with unknown name: ${methodName.toKString()}")
                    }
                        ?: throw DBusUnknownMethodException("Received call to method with no name ??? DBus should prevent this from happening")
                }
            }
        }
    }

    /**
     * Adds SDP record for RFCOMM service
     */
    fun sdpRegister() {

        val profileBuilder = g_variant_builder_new(G_VARIANT_TYPE_ARRAY)
        g_variant_builder_add(profileBuilder, "{sv}", "Channel", g_variant_new_uint16(11))
        g_variant_builder_add(profileBuilder, "{sv}", "Name", g_variant_new_string(SERVICE_NAME))
        g_variant_builder_add(profileBuilder, "{sv}", "Service", g_variant_new_string(SERIAL_PORT_PROFILE_UUID))
        //Todo: Add more data to sdp record

        val params = g_variant_new("(osa{sv})", PROFILE_OBJECT_PATH, CUSTOM_UUID, profileBuilder)

        g_variant_builder_unref(profileBuilder)



        params?.let {
            registerBluetoothSdp(it)
            registerBluetoothProfileObject()
        } ?: throw Exception("Error creating Bluetooth SDP profile")
    }

    /**
     * Registers bluetooth profile object which will receive new connection calls
     */
    private fun registerBluetoothProfileObject() {

        val vTable = cValue<GDBusInterfaceVTable> {
            method_call = connectionMethodCall
        }
        memScoped {
            val interfaceInfo = GLib.getInterfaceInfo(DBUS_PROFILE_OBJECT_DESCRIPTION, "org.bluez.Profile1")
            GLib.registerObject(PROFILE_OBJECT_PATH, interfaceInfo, vTable.ptr, null, null)
        }
    }

    /**
     * Call bluez profile register method with provided parameters
     * @param params parameters for the profile register function
     */
    private fun registerBluetoothSdp(params: CPointer<GVariant>) {
        GLib.methodCall(
            BLUEZ_BUS_NAME,
            "/org/bluez",
            "org.bluez.ProfileManager1",
            "RegisterProfile",
            params
        )
    }

}