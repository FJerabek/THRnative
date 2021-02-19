package cz.fjerabek.thr.data.enums

interface IControlProperty {
    fun getPropertyId() : Byte
    fun getMinimumValue() : Int
    fun getMaximumValue() : Int
}