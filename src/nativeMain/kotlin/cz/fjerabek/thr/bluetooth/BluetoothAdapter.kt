@file:Suppress("EXPERIMENTAL_API_USAGE") // Ignore unsigned types usage

package cz.fjerabek.thr.bluetooth

import cz.fjerabek.thr.glib.GLib

/**
 * Object representing bluetooth adapter. Connected to DBUS api
 */
object BluetoothAdapter {
    private const val INTERFACE_NAME = "org.bluez.Adapter1"

    /**
     * Is bluetooth adapter discovering
     */
    val discovering: Boolean
        get() = getParam("Discovering") as Boolean

    /**
     * Bluetooth adapter address
     */
    val address: String
        get() = getParam("Address") as String

    /**
     * Bluetooth adapter address type
     */
    val addressType: String
        get() = getParam("AddressType") as String

    /**
     * Bluetooth adapter mod alias
     */
    val modalias: String
        get() = getParam("Modalias") as String

    /**
     * Bluetooth adapter name
     */
    val name: String
        get() = getParam("Name") as String

    /**
     * Bluetooth adapter class
     */
    val clazz: UInt
        get() = getParam("Class") as UInt

    /**
     * Is bluetooth adapter discoverable
     */
    var discoverable: Boolean
        get() = getParam("Discoverable") as Boolean
        set(value) = setParam("Discoverable", value)

    /**
     * Bluetooth adapter discoverable timeout
     */
    var discoverableTimeout: UInt
        get() = getParam("DiscoverableTimeout") as UInt
        set(value) = setParam("DiscoverableTimeout", value)


    /**
     * If bluetooth adapter is pairable
     */
    var pairable: Boolean
        get() = getParam("Pairable") as Boolean
        set(value) = setParam("Pairable", value)

    /**
     * Bluetooth adapter pairable timeout
     */
    var pairableTimeout: UInt
        get() = getParam("PairableTimeout") as UInt
        set(value) = setParam("PairableTimeout", value)

    /**
     * Is bluetooth adapter turned on
     */
    var powered: Boolean
        get() = getParam("Powered") as Boolean
        set(value) = setParam("Powered", value)

    /**
     * Bluetooth adapter alias
     */
    var alias: String
        get() = getParam("Alias") as String
        set(value) = setParam("Alias", value)


    private fun getParam(name: String) =
        GLib.getParam(Bluetooth.BLUEZ_BUS_NAME, "/org/bluez/hci0", INTERFACE_NAME, name)

    private fun setParam(name: String, value: Any) =
        GLib.setParam(Bluetooth.BLUEZ_BUS_NAME, "/org/bluez/hci0", INTERFACE_NAME, name, value)
}
