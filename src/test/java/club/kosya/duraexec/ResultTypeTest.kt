package club.kosya.duraexec

import club.kosya.duraexec.ExecutionContext
import club.kosya.duraexec.internal.Execution
import club.kosya.duraexec.internal.ExecutionFlow
import club.kosya.duraexec.internal.ExecutionStatus
import club.kosya.duraexec.internal.ExecutionsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*

/**
 * Tests storing and restoring result types in ExecutionContext.
 */
class ResultTypeTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var executions: ExecutionsRepository
    private lateinit var execution: Execution

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        executions = mock(ExecutionsRepository::class.java)

        execution = Execution().apply {
            id = 1L
            status = ExecutionStatus.Running
            queuedAt = LocalDateTime.now()
            definition = byteArrayOf()
            params = "{}"
        }

        `when`(executions.findById(1L)).thenReturn(Optional.of(execution))
        `when`(executions.save(any(Execution::class.java))).thenReturn(execution)
    }

    @Test
    fun `test action stores result type for String`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        ctx.action("test") { "result" }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals("java.lang.String", flow.actions[0].resultType)
        assertEquals("\"result\"", flow.actions[0].result)
    }

    @Test
    fun `test action stores result type for Integer`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        ctx.action("test") { 42 }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals("java.lang.Integer", flow.actions[0].resultType)
        assertEquals("42", flow.actions[0].result)
    }

    @Test
    fun `test action stores result type for Path`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        val result = ctx.action("test") { Paths.get("/tmp/test.txt") }

        // Assert
        assertTrue(result is Path)
        assertEquals("/tmp/test.txt", result.toString())

        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals("sun.nio.fs.UnixPath", flow.actions[0].resultType)
    }

    @Test
    fun `test action stores result type for null`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        ctx.action("test") { null }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertNull(flow.actions[0].resultType)
        assertEquals("null", flow.actions[0].result)
    }

    @Test
    fun `test action stores result type for custom class`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)
        data class CustomResult(val value: String)

        // Act
        ctx.action("test") { CustomResult("test") }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertTrue(flow.actions[0].resultType!!.contains("CustomResult"))
    }

    @Test
    fun `test action stores result type for List`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        ctx.action("test") { listOf("a", "b", "c") }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertTrue(flow.actions[0].resultType!!.contains("List"))
    }
}
