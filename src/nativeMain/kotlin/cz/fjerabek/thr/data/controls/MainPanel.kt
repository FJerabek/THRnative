package cz.fjerabek.thr.data.controls

import cz.fjerabek.thr.data.enums.mainpanel.EAmpType
import cz.fjerabek.thr.data.enums.mainpanel.ECabinetType
import cz.fjerabek.thr.data.enums.mainpanel.EMainPanel
import kotlinx.serialization.Serializable

@Serializable
class MainPanel(
    val amp : EAmpType,
    val gain : Byte,
    val master : Byte,
    val bass : Byte,
    val middle : Byte,
    val treble : Byte,
    val cabinet : ECabinetType?
) : IControl {

//    var amp = amp
//        set(value) {
//            require(value.id <= EMainPanel.AMP.max && value.id >= EMainPanel.AMP.min) {"Invalid amp type"}
//            field = value
//        }
//
//    var gain = gain
//        set(value) {
//            require(value <= EMainPanel.GAIN.max && value >= EMainPanel.GAIN.min) {"Gain out of range"}
//            field = value
//        }
//
//    var master = master
//        set(value) {
//            require(value <= EMainPanel.MASTER.max && value >= EMainPanel.MASTER.min) {"Master out of range"}
//            field = value
//        }
//
//    var bass = bass
//        set(value) {
//            require(value <= EMainPanel.BASS.max && value >= EMainPanel.BASS.min) {"Bass out of range"}
//            field = value
//        }
//
//    var middle = middle
//        set(value) {
//            require(value <= EMainPanel.MIDDLE.max && value >= EMainPanel.MIDDLE.min) {"Middle out of range"}
//            field = value
//        }
//
//    var treble = treble
//        set(value) {
//            require(value <= EMainPanel.TREBLE.max && value >= EMainPanel.TREBLE.min) {"Treble out of range"}
//            field = value
//        }
//
//    var cabinet = cabinet
//        set(value) {
//            if(amp.id < EAmpType.BASS.id) {
//                if(value != null)
//                    require(value.id <= EMainPanel.CABINET.max && value.id >= EMainPanel.CABINET.min) { "Cabinet out of range" }
//                field = value
//            }
//        }

    override fun toDump(dump: ByteArray): ByteArray {
        dump[EMainPanel.AMP.dumpPosition] = amp.id
        dump[EMainPanel.GAIN.dumpPosition] = gain
        dump[EMainPanel.MASTER.dumpPosition] = master
        dump[EMainPanel.BASS.dumpPosition] = bass
        dump[EMainPanel.MIDDLE.dumpPosition] = middle
        dump[EMainPanel.TREBLE.dumpPosition] = treble
        dump[EMainPanel.CABINET.dumpPosition] = cabinet?.id ?: 0
        return dump
    }

    companion object {
        fun fromDump(dump: ByteArray) : MainPanel {

            val amp = EAmpType.fromId(dump[EMainPanel.AMP.dumpPosition])!!
            var cab : ECabinetType? = null
            if(amp.id < EAmpType.BASS.id) {
                cab = ECabinetType.fromId(dump[EMainPanel.CABINET.dumpPosition])
            }

            return MainPanel(
                amp,
                dump[EMainPanel.GAIN.dumpPosition],
                dump[EMainPanel.MASTER.dumpPosition],
                dump[EMainPanel.BASS.dumpPosition],
                dump[EMainPanel.MIDDLE.dumpPosition],
                dump[EMainPanel.TREBLE.dumpPosition],
                cab
            )
        }
    }
}