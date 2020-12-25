package cz.fjerabek.thr.bluetooth

import cz.fjerabek.thr.midi.messages.PresetMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface IBluetoothMessage

/**
 * Request for firmware version
 */
@Serializable
@SerialName("FwVersionRq")
class FwVersionRq: IBluetoothMessage

/**
 * Request for hardware status info
 */
@Serializable
@SerialName("HwStatusRq")
class HwStatusRq: IBluetoothMessage

/**
 * Request for all loaded presets
 */
@Serializable
@SerialName("PresetsRq")
class PresetsRq: IBluetoothMessage

/**
 * Request for current combo preset
 */
@Serializable
@SerialName("CurrentPresetRq")
class CurrentPresetRq: IBluetoothMessage

/**
 * Request for removal of presets
 * @param index preset index to remove
 */
@Serializable
@SerialName("RemovePresetRq")
data class RemovePresetRq(val index: Int): IBluetoothMessage

/**
 * Preset addition request
 * @param preset preset to add
 */
@Serializable
@SerialName("AddPresetRq")
data class AddPresetRq(val preset: PresetMessage): IBluetoothMessage

/**
 * Request for replacing all presets with custom ones
 * @param presets custom presets
 */
@Serializable
@SerialName("SetPresetsRq")
data class SetPresetsRq(val presets: List<PresetMessage>): IBluetoothMessage

/**
 * Response message with all loaded presets
 * @param presets loaded presets
 */
@Serializable
@SerialName("PresetsResponse")
data class PresetsResponse(val presets: List<PresetMessage>): IBluetoothMessage

/**
 * Request for setting preset on combo
 * @param index preset index
 */
@Serializable
@SerialName("PresetSelect")
data class PresetSelect(val index: Int): IBluetoothMessage

