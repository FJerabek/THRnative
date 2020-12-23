package cz.fjerabek.thr.bluetooth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface IBluetoothMessage

@Serializable
@SerialName("FwVersionRq")
class FwVersionRq: IBluetoothMessage

@Serializable
@SerialName("HwStatusRq")
class HwStatusRq: IBluetoothMessage
