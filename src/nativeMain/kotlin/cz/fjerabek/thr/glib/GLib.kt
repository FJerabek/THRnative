@file:Suppress("EXPERIMENTAL_API_USAGE")// Ignore unsigned types usage warnings

package cz.fjerabek.thr.glib

import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import glib.*
import kotlinx.cinterop.*


/**
 * Glib exception
 * @param message exception message
 */
open class GLibException(message: String) : Exception(message)

/**
 * Exception when registering object into DBUS
 * @param message exception message
 */
class GLibObjectRegisterException(message: String) : GLibException(message)

/**
 * Exception when variant has unsupported data type
 * @param message exception message
 */
class GLibVariantUnsupportedData(message: String) : GLibException(message)

/**
 * Exception when requested parameter does not exist on object
 * @param message exception message
 */
class GLibParameterException(message: String) : GLibException(message)

/**
 * Exception when creating node from xml
 * @param message exception message
 */
class GLibInterfaceInfoException(message: String): GLibException(message)

/**
 * Exception when calling dbus method
 * @param message exception message
 */
class GLibMethodCallException(message: String): GLibException(message)

/**
 * Exception when dbus method call is received with unknown method name
 * @param message exception message
 */
class DBusUnknownMethodException(message: String): GLibException(message)

/**
 * Utility library wrapping GLib C library used for DBUS communication
 */
object GLib {

    private val dbus: CPointer<GDBusConnection>? = memScoped {
        val error = allocPointerTo<GError>()
        val connection = g_bus_get_sync(G_BUS_TYPE_SYSTEM, null, error.ptr)
        error.value?.let { //Check for error while opening dbus
            "Error opening dbus: ${it.pointed.message?.toKString()}".error()
        }

//        requestBusName(connection, "cz.fjerabek.thr")

        g_dbus_connection_get_unique_name(connection)?.let {
            "Unique dbus name: ${it.toKString()}".debug()
        } ?: "Error getting unique name".error()
        connection
    }


    /**
     * Requests name for bus
     * @param connection dbus connection
     * @param name requested name
     */
    private fun requestBusName(connection: CPointer<GDBusConnection>?, name: String) {
        memScoped {
            val error = allocPointerTo<GError>()
            g_dbus_connection_call_sync(
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
            throw GLibVariantUnsupportedData("Unsupported data type: $data")
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
                throw GLibVariantUnsupportedData("Unsupported data type: $type")
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
        val params = g_variant_new("(ss)", interfaceName.cstr, paramName.cstr)
        return memScoped {
            val error = allocPointerTo<GError>()
            val response = g_dbus_connection_call_sync(
                dbus,
                busName,
                objectPath,
                "org.freedesktop.DBus.Properties",
                "Get",
                params,
                g_variant_type_checked_("(v)"),
                G_DBUS_SEND_MESSAGE_FLAGS_NONE,
                -1,
                null,
                error.ptr
            )

            response?.let {
                getVariantData(it)
            } ?: {
                error.value?.let {
                    throw GLibParameterException("Parameter get error: ${it.pointed.message?.toKString()}")
                }
            }
        }
    }

    /**
     * Sets parameter in DBUS object specified by objectPath and paramName
     * @param objectPath path to the DBUS object
     * @param interfaceName name of the interface
     * @param paramName parameter name
     * @param data data to set to parameter. Must be in correct type
     */
    fun setParam(busName: String, objectPath: String, interfaceName: String, paramName: String, data: Any) {
        val params = g_variant_new("(ssv)", interfaceName.cstr, paramName.cstr, createVariant(data))
        memScoped {
            val error = allocPointerTo<GError>()
            g_dbus_connection_call_sync(
                dbus,
                busName,
                objectPath,
                "org.freedesktop.DBus.Properties",
                "Set",
                params,
                null,
                G_DBUS_SEND_MESSAGE_FLAGS_NONE,
                -1,
                null,
                error.ptr
            )
            error.value?.let {
                throw GLibParameterException("Error setting parameter ${it.pointed.message?.toKString()}")
            }
        }
    }

    /**
     * Method wrapping call to the interface lookup method
     * @param interfaceIntrospectDescription Interface introspection string data
     * @param name interface name
     */
    fun getInterfaceInfo(
        interfaceIntrospectDescription: String,
        name: String
    ): CPointer<GDBusInterfaceInfo> = memScoped {
        val error = allocPointerTo<GError>()
        val nodeInfo = g_dbus_node_info_new_for_xml(interfaceIntrospectDescription, error.ptr)
        error.value?.let {
            throw GLibInterfaceInfoException("Error creating node from xml ${it.pointed.message?.toKString()}")
        }
        g_dbus_node_info_lookup_interface(nodeInfo, name)!!
    }

    /**
     * Wraps calling to dbus register object method
     * @param objectPath path for the object registration
     * @param interfaceInfo info about implemented interface
     * @param vTable vector table for method calls
     * @param userData user data passed to methods
     * @param userFreeFunc method called to free user data
     */
    fun registerObject(
        objectPath: String,
        interfaceInfo: CValuesRef<GDBusInterfaceInfo>,
        vTable: CValuesRef<GDBusInterfaceVTable>,
        userData: gpointer?,
        userFreeFunc: GDestroyNotify?
    ) {
        memScoped {
            val error = allocPointerTo<GError>()
            g_dbus_connection_register_object(
                dbus,
                objectPath,
                interfaceInfo,
                vTable,
                userData,
                userFreeFunc,
                error.ptr
            )
            error.pointed?.let {
                throw GLibObjectRegisterException(
                    it.message?.toKString() ?: "Object register exception without error message"
                )
            }
        }
    }

    /**
     * Synchronous call to dbus method
     * @param busName name of the bus on which the method is
     * @param objectPath path to the object to call method on
     * @param interfaceName interface name to which the method belong to
     * @param methodName name of the method to call
     * @param parameters method parameters
     * @param replyType variant type representing the response type
     */
    fun methodCallWithReply(
        busName: String,
        objectPath: String,
        interfaceName: String,
        methodName: String,
        parameters: CValuesRef<GVariant>?,
        replyType: CValuesRef<GVariantType>?
    ): CPointer<GVariant>? = memScoped {
        val error = allocPointerTo<GError>()
        val reply = g_dbus_connection_call_sync(
            dbus,
            busName,
            objectPath,
            interfaceName,
            methodName,
            parameters,
            replyType,
            G_DBUS_CALL_FLAGS_NONE,
            -1,
            null,
            error.ptr
        )
        error.value?.let {
            throw GLibMethodCallException(it.pointed.message?.toKString() ?: "Method call error without message")
        }
        reply
    }

    /**
     * Synchronous call to dbus method with no return value
     * @param busName name of the bus on which the method is
     * @param objectPath path to the object to call method on
     * @param interfaceName interface name to which the method belong to
     * @param methodName name of the method to call
     * @param parameters method parameters
     */
    fun methodCall(
        busName: String,
        objectPath: String,
        interfaceName: String,
        methodName: String,
        parameters: CValuesRef<GVariant>?
    ) {
        methodCallWithReply(
            busName,
            objectPath,
            interfaceName,
            methodName,
            parameters,
            null
        )
    }
}