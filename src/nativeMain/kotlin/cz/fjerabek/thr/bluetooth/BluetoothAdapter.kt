@file:Suppress("EXPERIMENTAL_API_USAGE") // Ignore unsigned types usage

package cz.fjerabek.thr.bluetooth

import cz.fjerabek.thr.glib.GLib

object BluetoothAdapter {
    private const val INTERFACE_NAME = "org.bluez.Adapter1"

    val discovering: Boolean
        get() = getParam("Discovering") as Boolean

    val address: String
        get() = getParam("Address") as String

    val addressType: String
        get() = getParam("AddressType") as String

    val modalias: String
        get() = getParam("Modalias") as String

    val name: String
        get() = getParam("Name") as String

    val clazz: UInt
        get() = getParam("Class") as UInt

    var discoverable: Boolean
        get() = getParam("Discoverable") as Boolean
        set(value) = setParam("Discoverable", value)

    var discoverableTimeout: UInt
        get() = getParam("DiscoverableTimeout") as UInt
        set(value) = setParam("DiscoverableTimeout", value)

    var pairable: Boolean
        get() = getParam("Pairable") as Boolean
        set(value) = setParam("Pairable", value)

    var pairableTimeout: UInt
        get() = getParam("PairableTimeout") as UInt
        set(value) = setParam("PairableTimeout", value)

    var powered: Boolean
        get() = getParam("Powered") as Boolean
        set(value) = setParam("Powered", value)

    var alias: String
        get() = getParam("Alias") as String
        set(value) = setParam("Alias", value)


    private fun getParam(name: String) =
        GLib.getParam(Bluetooth.BLUEZ_BUS_NAME, "/org/bluez/hci0", INTERFACE_NAME, name)

    private fun setParam(name: String, value: Any) =
        GLib.setParam(Bluetooth.BLUEZ_BUS_NAME, "/org/bluez/hci0", INTERFACE_NAME, name, value)
}
