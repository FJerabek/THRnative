package cz.fjerabek.thr.glib

import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.LogUtils.info
import glib.*
import kotlinx.cinterop.*
import platform.posix.sleep


object GLib {
    private val dbus: CPointer<GDBusConnection>?

    init {
        dbus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, null, null)
        if (dbus == null)
            "Error opening dbus".error()
    }

    fun createVariant(data: Any) = when(data) {
        is Boolean -> g_variant_new("b", data as Boolean)
        is Int -> g_variant_new("i", data as Int) //Must have type or compiler throws error
        is UInt -> g_variant_new("u", data as UInt)
        is Short -> g_variant_new("n", data as Short)
        is UShort -> g_variant_new("q", data as UShort)
        is Long -> g_variant_new("n", data as Long)
        is ULong -> g_variant_new("t", data as ULong)
        is String -> g_variant_new("s", data as String)
        else -> {
            "Unsupported data type: $data".error()
            g_variant_new("")
        }
    }

    private fun getVariantData(variant: CPointer<GVariant>): Any? {
        val type = g_variant_get_type_string(variant)?.toKString()
        return when(type) {
            "b" -> {
                g_variant_get_boolean(variant) == 1
            }
            "" -> {
                null
            }
            "s" -> {
                g_variant_get_string(variant, null)?.toKString()
            }
            else -> {
                "Parameter type ($type) not supported".error()
                null
            }
        }
    }

    /**
     * Gets parameter from DBUS object specified by objectPath and paramName
     * @param busName name of the bus
     * @param objectPath path to the DBUS object
     * @param interfaceName name of the interface
     * @param paramName parameter name
     * @return return parameter value with parameter type
     */
    fun getParam(busName: String, objectPath: String, interfaceName: String, paramName: String): Any? {
        val method =
            g_dbus_message_new_method_call(
                busName,
                objectPath,
                "org.freedesktop.DBus.Properties",
                "Get"
            ) //Create DBUS method call object

        val params = g_variant_new("(ss)", interfaceName.cstr, paramName.cstr)
        g_dbus_message_set_body(method, params) //Set method parameters

        val reply = g_dbus_connection_send_message_with_reply_sync(
            dbus,
            method,
            G_DBUS_SEND_MESSAGE_FLAGS_NONE,
            -1,
            null,
            null,
            null
        )//Get method reply

        g_dbus_message_get_error_name(reply)?.let {
            "Error sending DBUS get method call: ${it.toKString()}".error()
            return null
        }

        g_dbus_message_get_body(reply)?.let {
            return memScoped {
                val variant = allocPointerTo<GVariant>()
                g_variant_get(it, "(v)", variant)

               variant.value?.let {
                    getVariantData(it)
                }
            }
        } ?: "Param get error: ${g_dbus_message_get_error_name(reply)?.toKString()}".error()

        //Todo: Free param, method object and reply
//        g_free(params)
//        g_free(method)
        return null
    }

    fun setParam(busName: String, objectPath: String, interfaceName: String, paramName: String, data: Any) {
        val method =
            g_dbus_message_new_method_call(
                busName,
                objectPath,
                "org.freedesktop.DBus.Properties",
                "Set"
            ) //Create DBUS method call object

        val params = g_variant_new("(ssv)", interfaceName.cstr, paramName.cstr, createVariant(data))
        g_dbus_message_set_body(method, params) //Set method parameters

        val reply = g_dbus_connection_send_message_with_reply_sync(
            dbus,
            method,
            G_DBUS_SEND_MESSAGE_FLAGS_NONE,
            -1,
            null,
            null,
            null
        )
        g_dbus_message_get_error_name(reply)?.let {
            "Error sending DBUS get method call: ${it.toKString()}".error()
        }
        //Todo: Free param, method object and reply
    }

    fun send() {
        getParam("org.bluez", "/org/bluez/hci0", "org.bluez.Adapter1", "Discoverable").toString().info()
        setParam("org.bluez", "/org/bluez/hci0", "org.bluez.Adapter1", "Discoverable", false)
        sleep(10)
        getParam("org.bluez", "/org/bluez/hci0", "org.bluez.Adapter1", "Discoverable").toString().info()
//        val method =
//            g_dbus_message_new_method_call("org.bluez", "/org/bluez", "org.freedesktop.DBus.Introspectable", "Introspect")
//        val reply = g_dbus_connection_send_message_with_reply_sync(dbus, method, G_DBUS_SEND_MESSAGE_FLAGS_NONE, -1, null, null, null)
//        val body = g_dbus_message_get_body(reply)
//        val type = g_variant_get_type_string(body)?.toKString()
//        debug { "Variant type: $type" }
//
//        memScoped {
//            val string = allocPointerTo<ByteVar>()
//
//            g_variant_get(body, "(s)", string)
//            debug { "String value: ${string.value?.toKString()}" }
//            g_variant_is_of_type(body, G_VARIANT_TYPE_STRING).toString().debug()
//            g_variant_get_string(body, size.ptr)?.toKString()?.debug()
//        }
//        g_object_unref(method)

//        val size = cValue<gsizeVar>()
//        val string = memScoped {
//            g_variant_get_string(g_dbus_message_get_body(reply),size.ptr)
//        }
//        "method called response: ${string?.toKString()}".debug()
    }
}