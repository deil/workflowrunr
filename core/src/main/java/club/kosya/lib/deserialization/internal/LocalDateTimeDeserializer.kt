package club.kosya.lib.deserialization.internal

import club.kosya.lib.deserialization.ResultDeserializer
import java.time.LocalDateTime

class LocalDateTimeDeserializer : ResultDeserializer {
    override fun deserialize(
        typeName: String?,
        value: String?,
    ): Any? {
        if (value == null) {
            return null
        }

        return LocalDateTime.parse(value)
    }
}
