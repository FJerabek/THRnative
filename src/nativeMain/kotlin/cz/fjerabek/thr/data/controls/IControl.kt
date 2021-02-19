package cz.fjerabek.thr.data.controls

interface IControl {
    fun toDump(dump : ByteArray) : ByteArray
}