package club.kosya.lib.deserialization

import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ResultDeserializerRegistryTest {
    private lateinit var registry: ObjectDeserializer

    @BeforeEach
    fun setUp() {
        registry = ObjectDeserializerImpl(ObjectMapper())
    }

    @Nested
    inner class NullHandling {
        @Test
        fun `should return null when type is null`() {
            // Arrange & Act
            val result = registry.deserialize(null, "\"value\"")

            // Assert
            assertNull(result)
        }

        @Test
        fun `should return null when value is null`() {
            // Arrange & Act
            val result = registry.deserialize("java.lang.String", null)

            // Assert
            assertNull(result)
        }

        @Test
        fun `should return null when both type and value are null`() {
            // Arrange & Act
            val result = registry.deserialize(null, null)

            // Assert
            assertNull(result)
        }
    }

    @Nested
    inner class JacksonDeserialization {
        @Test
        fun `should deserialize string`() {
            // Arrange & Act
            val result = registry.deserialize("java.lang.String", "\"test\"")

            // Assert
            assertEquals("test", result)
        }

        @Test
        fun `should deserialize integer`() {
            // Arrange & Act
            val result = registry.deserialize("java.lang.Integer", "42")

            // Assert
            assertEquals(42, result)
        }

        @Test
        fun `should deserialize boolean`() {
            // Arrange & Act
            val result = registry.deserialize("java.lang.Boolean", "true")

            // Assert
            assertEquals(true, result)
        }
    }

    @Nested
    inner class CustomDeserialization {
        @Test
        fun `should handle Path type`() {
            // Arrange & Act
            val result = registry.deserialize("sun.nio.fs.UnixPath", "\"file:///tmp/test.txt\"")

            // Assert
            assertEquals("/tmp/test.txt", result.toString())
            assertTrue(result is java.nio.file.Path)
        }
    }
}
