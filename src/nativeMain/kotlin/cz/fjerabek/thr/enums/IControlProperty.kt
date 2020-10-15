package cz.fjerabek.thr.enums

interface IControlProperty {
    fun getPropertyId() : Byte
    fun getMinimumValue() : Int
    fun getMaximumValue() : Int
}