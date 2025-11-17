package club.kosya.lib.deserialization.internal

import club.kosya.lib.deserialization.ResultDeserializer

class PrimitiveDeserializer : ResultDeserializer {
    override fun deserialize(
        typeName: String?,
        value: String?,
    ): Any? {
        if (value == null) {
            return null
        }

        return when (typeName) {
            "java.lang.Integer", "int" -> value.toInt()
            "java.lang.Long", "long" -> value.toLong()
            "java.lang.Double", "double" -> value.toDouble()
            "java.lang.Float", "float" -> value.toFloat()
            "java.lang.Boolean", "boolean" -> value.toBoolean()
            "java.lang.Short", "short" -> value.toShort()
            "java.lang.Byte", "byte" -> value.toByte()
            "java.lang.Character", "char" -> value.firstOrNull()
            else -> throw IllegalArgumentException("Unsupported primitive type: $typeName")
        }
    }
}
