@file:Suppress("EXPERIMENTAL_API_USAGE") // Ignore unsigned types usage

package cz.fjerabek.thr.bluetooth

import cz.fjerabek.thr.glib.GLib

object BluetoothAdapter {
    private const val INTERFACE_NAME = "org.bluez.Adapter1"

    val discovering: Boolean
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Discovering") as Boolean

    val address: String
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Address") as String

    val addressType: String
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "AddressType") as String

    val modalias: String
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Modalias") as String

    val name: String
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Name") as String

    val clazz: UInt
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Class") as UInt

    var discoverable: Boolean
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Discoverable") as Boolean
        set(value) = GLib.setParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Discoverable", value)

   var discoverableTimeout: UInt
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "DiscoverableTimeout") as UInt
        set(value) = GLib.setParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "DiscoverableTimeout", value)

    var pairable: Boolean
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Pairable") as Boolean
        set(value) = GLib.setParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Pairable", value)

    var pairableTimeout: UInt
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "PairableTimeout") as UInt
        set(value) = GLib.setParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "PairableTimeout", value)

    var powered: Boolean
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Powered") as Boolean
        set(value) = GLib.setParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Powered", value)

    var alias: String
        get() = GLib.getParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Alias") as String
        set(value) = GLib.setParam("org.bluez", "/org/bluez/hci0", INTERFACE_NAME, "Alias", value)

}
