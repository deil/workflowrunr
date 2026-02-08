package club.kosya.lib.deserialization

interface ResultDeserializer {
    fun deserialize(
        typeName: String?,
        value: String?,
    ): Any?
}
