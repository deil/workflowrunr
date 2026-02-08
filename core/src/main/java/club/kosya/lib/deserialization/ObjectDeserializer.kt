package club.kosya.lib.deserialization

interface ObjectDeserializer {
    fun deserialize(
        typeName: String?,
        value: String?,
    ): Any?
}
