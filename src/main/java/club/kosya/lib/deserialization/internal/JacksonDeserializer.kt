package club.kosya.lib.deserialization.internal

import club.kosya.lib.deserialization.ResultDeserializer
import com.fasterxml.jackson.databind.ObjectMapper

class JacksonDeserializer(
    private val objectMapper: ObjectMapper,
) : ResultDeserializer {
    override fun deserialize(
        typeName: String?,
        value: String?,
    ): Any? {
        if (value == null) {
            return null
        }

        val type = Class.forName(typeName!!)
        return objectMapper.readValue(value, type)
    }
}
