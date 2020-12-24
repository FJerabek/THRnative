package cz.fjerabek.thr.file

import com.badoo.reaktive.utils.atomic.AtomicReference
import cz.fjerabek.thr.LogUtils.debug
import cz.fjerabek.thr.LogUtils.error
import cz.fjerabek.thr.midi.messages.PresetMessage
import cz.fjerabek.thr.serializer
import kotlinx.cinterop.*
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import platform.posix.*

object PresetsManager {

    fun savePresets(presetFilePath: String, presets: List<PresetMessage>) {
        val file = fopen(presetFilePath, "wb")
        if (file == NULL) {
            "PresetManager save error: ${strerror(errno)?.toKString()}".error()
        }
        val string = serializer.encodeToString(presets)
        fputs(string, file)
        fclose(file)
    }

    fun loadPresets(presetFilePath: String): List<PresetMessage> {
        val file = fopen(presetFilePath, "rb")
        if (file == NULL) {
            "PresetManager load error: ${strerror(errno)?.toKString()}".error()
        }
        fseek(file, 0, SEEK_END)
        val length: Long = ftell(file)
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