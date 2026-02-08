package club.kosya.lib.deserialization.internal

import club.kosya.lib.deserialization.ResultDeserializer
import com.fasterxml.jackson.databind.ObjectMapper

class StringDeserializer(
    private val objectMapper: ObjectMapper,
) : ResultDeserializer {
    override fun deserialize(
        typeName: String?,
        value: String?,
    ): Any? {
        if (value == null) {
            return null
        }

        // If value is JSON-encoded (starts with quote), parse it as JSON
        // Otherwise, return as raw value
        return if (value.startsWith("\"") && value.endsWith("\"")) {
            objectMapper.readValue(value, String::class.java)
        } else {
            value
        }
    }
}
