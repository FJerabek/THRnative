package cz.fjerabek.thr.bluetooth

import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.glib.DBusUnknownMethodException
import cz.fjerabek.thr.glib.GLib
import glib.*
import kotlinx.cinterop.*
import platform.posix.uint32_t

object PinAgent {
    private const val AGENT_DBUS_PATH = "/cz/fjerabek/thr/agent"
    private const val DBUS_AGENT_OBJECT_DESCRIPTION =
        """
            <node>
                <interface name="org.bluez.Agent1">
                    <method name="Release" />
                    <method name="RequestPinCode">
                        <arg direction="in" type="o" />
                        <arg direction="out" type="s" />
                    </method>
                    <method name="DisplayPinCode">
                        <arg direction="in" type="o" />
                        <arg direction="in" type="s" />
                    </method>
                    <method name="RequestPasskey">
                        <arg direction="in" type="o" />
                        <arg direction="out" type="u" />
                    </method>
                    <method name="DisplayPasskey">
                        <arg direction="in" type="o" />
                        <arg direction="in" type="u" />
                        <arg direction="in" type="q" />
                    </method>
                    <method name="RequestConfirmation">
                        <arg direction="in" type="o" />
                        <arg direction="in" type="u" />
                    </method>
                    <method name="RequestAuthorization">
                        <arg direction="in" type="o" />
                    </method>
                    <method name="AuthorizeService">
                        <arg direction="in" type="o" />
                        <arg direction="in" type="s" />
                    </method>
                    <method name="Cancel" />
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
    { _/*DBus*/,
      _/*sender*/,
      _/*object name*/,
      _/*interface name*/,
      methodName,
      parameters,
      invocation/*invocation*/,
      _/*User Data*/ ->
        memScoped {
            val device = allocPointerTo<gcharVar>()
            "Received method call to: ${methodName!!.toKString()}".debug()
            when (methodName.toKString()) {
                "Cancel" ->  { }
                "Release" -> { }
                "RequestPinCode" -> { }
                "DisplayPinCode" -> { }
                "RequestPasskey" -> { }
                "DisplayPasskey" -> { }
                "RequestConfirmation" -> { }
                "AuthorizeService" -> { }
                "RequestAuthorization" -> {
                    g_variant_get(parameters, "(o)", device.ptr)
                    requestAuthorization(device.value!!.toKString(), invocation)
                }
                else -> throw DBusUnknownMethodException("Call to unknown method ${methodName.toKString()}")
            }
        }
    }

    private fun requestAuthorization(device: String, invocation: CPointer<GDBusMethodInvocation>?) {
        """
            RequestAuthorization
            device: $device
        """.trimIndent().debug()
        g_dbus_method_invocation_return_value(invocation, null)
        BluetoothAdapter.pairable = false
        BluetoothAdapter.discoverable = false
    }

    fun registerAgent() {
        val vTable = cValue<GDBusInterfaceVTable> {
            method_call = connectionMethodCall
        }
        memScoped {
            val error = allocPointerTo<GError>()
            val nodeInfo = g_dbus_node_info_new_for_xml(DBUS_AGENT_OBJECT_DESCRIPTION, error.ptr)
            error.value?.let {

            }

            val interfaceInfo = g_dbus_node_info_lookup_interface(nodeInfo, "org.bluez.Agent1")!!

            GLib.registerObject(AGENT_DBUS_PATH, interfaceInfo, vTable.ptr, null, null)
            GLib.methodCall(
                Bluetooth.BLUEZ_BUS_NAME,
                "/org/bluez",
                "org.bluez.AgentManager1",
                "RegisterAgent",
                g_variant_new("(os)", AGENT_DBUS_PATH, "NoInputNoOutput")
            )
            GLib.methodCall(
                Bluetooth.BLUEZ_BUS_NAME,
                "/org/bluez",
                "org.bluez.AgentManager1",
                "RequestDefaultAgent",
                g_variant_new("(o)", AGENT_DBUS_PATH)
            )
        }
    }
}