@file:Suppress("EXPERIMENTAL_API_USAGE")// Ignore unsigned types usage warnings

package cz.fjerabek.thr.glib

import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.LogUtils.info
import cz.fjerabek.thr.bluetooth.Bluetooth
import glib.*
import kotlinx.cinterop.*
import platform.posix.errno
import platform.posix.strerror
import platform.posix.write

/**
 * Utility library wrapping GLib C library used for DBUS communication
 */
object GLib {
     val dbus: CPointer<GDBusConnection>? = memScoped {
        val error = allocPointerTo<GError>()
        val connection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, null, error.ptr)
        error.value?.let { //Check for error while opening dbus
            "Error opening dbus: ${it.pointed.message?.toKString()}".error()
        }

        requestBusName(connection, "cz.fjerabek.thr")

        g_dbus_connection_get_unique_name(connection)?.let {
            "Unique dbus name: ${it.toKString()}".debug()
        } ?: "Error getting unique name".error()
        connection
    }

    /**
     * Requests name for bus
     * @param
     */
    private fun requestBusName(connection: CPointer<GDBusConnection>?, name: String) {
        memScoped {
            val error = allocPointerTo<GError>()
            val reply = g_dbus_connection_call_sync(
                connection,
                "org.freedesktop.DBus",
                "/",
                "org.freedesktop.DBus",
                "RequestName",
                g_variant_new("(su)", name.cstr, G_BUS_NAME_OWNER_FLAGS_ALLOW_REPLACEMENT),
                null,
                G_DBUS_CALL_FLAGS_NONE,
                -1,
                null,
                error.ptr
            )//Request name for bus connection

            error.value?.let { //Check for error while opening dbus
                "Error requesting bus name: ${it.pointed.message?.toKString()}".error()
            }
        }
    }

    /**
     * Creates variant containing data
     * @param data containing data in supported type
     * @return variant with data
     */
    @Suppress("USELESS_CAST") //The cast is needed because CInterop does not know what type to use
    private fun createVariant(data: Any) = when (data) {
        is Boolean -> g_variant_new("b", data as Boolean)
        is Int -> g_variant_new("i", data as Int)
        is UInt -> g_variant_new("u", data as UInt)
        is Short -> g_variant_new("n", data as Short)
        is UShort -> g_variant_new("q", data as UShort)
        is Long -> g_variant_new("x", data as Long)
        is ULong -> g_variant_new("t", data as ULong)
        is String -> g_variant_new("s", data as String)
        else -> {
            "Unsupported data type: $data".error()
            g_variant_new("")
        }
    }

    /**
     * Returns data from variant
     * @param variant variant to get data from
     * @return data in variant
     */
    private fun getVariantData(variant: CPointer<GVariant>): Any? {
        return when (val type = g_variant_get_type_string(variant)?.toKString()) {
            "b" -> g_variant_get_boolean(variant) == 1
            "i" -> g_variant_get_int32(variant)
            "u" -> g_variant_get_uint32(variant)
            "n" -> g_variant_get_int16(variant)
            "q" -> g_variant_get_uint16(variant)
            "x" -> g_variant_get_int64(variant)
            "t" -> g_variant_get_uint64(variant)
            "s" -> g_variant_get_string(variant, null)?.toKString()
            "" -> null
            else -> {
                "Parameter type ($type) not supported".error()
                null
            }
        }
    }

    /**
     * Gets parameter from DBUS object specified by objectPath and paramName
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

    /**
     * Sets parameter in DBUS object specified by objectPath and paramName
     * @param objectPath path to the DBUS object
     * @param interfaceName name of the interface
     * @param paramName parameter name
     * @param data data to set to parameter. Must be in correct type
     */
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
}