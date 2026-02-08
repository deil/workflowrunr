package club.kosya.lib.deserialization.internal

import club.kosya.lib.deserialization.ObjectDeserializer
import com.fasterxml.jackson.databind.ObjectMapper

class ObjectDeserializerImpl(
    private val objectMapper: ObjectMapper,
) : ObjectDeserializer {
    private val stringDeserializer = StringDeserializer(objectMapper)
    private val primitiveDeserializer = PrimitiveDeserializer()

    private val customDeserializers =
        mapOf(
            "sun.nio.fs.UnixPath" to PathDeserializer(),
            "java.lang.String" to stringDeserializer,
        )

    private val primitiveTypes =
        setOf(
            "java.lang.Integer",
            "int",
            "java.lang.Long",
            "long",
            "java.lang.Double",
            "double",
            "java.lang.Float",
            "float",
            "java.lang.Boolean",
            "boolean",
            "java.lang.Short",
            "short",
            "java.lang.Byte",
            "byte",
            "java.lang.Character",
            "char",
        )

    override fun deserialize(
        typeName: String?,
        value: String?,
    ): Any? {
        if (typeName == null || value == null) {
            return null
        }

        if (typeName in primitiveTypes) {
            return primitiveDeserializer.deserialize(typeName, value)
        }

        val deserializer = customDeserializers[typeName] ?: JacksonDeserializer(objectMapper)
        return deserializer.deserialize(typeName, value)
    }
}
