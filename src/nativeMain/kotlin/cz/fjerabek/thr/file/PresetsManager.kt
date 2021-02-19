package cz.fjerabek.thr.file

import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.LogUtils.warn
import cz.fjerabek.thr.data.midi.PresetMessage
import cz.fjerabek.thr.serializer
import kotlinx.cinterop.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import platform.posix.*

object PresetsManager {

    fun savePresets(presetFilePath: String, presets: List<PresetMessage>) {
        val file = fopen(presetFilePath, "wb")
        if (file == null) {
            "PresetManager save error: ${strerror(errno)?.toKString()}".error()
        } else {
            val string = serializer.encodeToString(presets)
            fputs(string, file)
            fclose(file)
        }
    }

    fun loadPresets(presetFilePath: String): List<PresetMessage>? {
        val file = fopen(presetFilePath, "rb")
        if (file == null) {
            "PresetManager load error: ${strerror(errno)?.toKString()}".warn()
            return null
        }
        fseek(file, 0, SEEK_END)
        val length: Long = ftell(file).convert()
        fseek(file, 0, SEEK_SET)
        val buffer = ByteArray(length.toInt())
        buffer.usePinned {
            fread(it.addressOf(0), 1, length.convert(), file)
        }
        val contents = buffer.decodeToString()
        fclose(file)
        return serializer.decodeFromString(contents)
    }
}