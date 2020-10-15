package cz.fjerabek.thr.controls

interface IControl {
    fun toDump(dump : ByteArray) : ByteArray
}