package club.kosya.lib.deserialization.internal

import club.kosya.lib.deserialization.ResultDeserializer
import java.nio.file.Path

class PathDeserializer : ResultDeserializer {
    override fun deserialize(
        typeName: String?,
        value: String?,
    ): Any? {
        if (value == null) {
            return null
        }

        return Path.of(value.removeSurrounding("\"").replace("file://", ""))
    }
}
