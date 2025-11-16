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
import java.time.LocalDateTime
import java.util.*

/**
 * Tests action result persistence and flow restoration.
 */
class ResultPersistenceTest {

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
    fun `test action stores result in ExecutedAction`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        val result = ctx.action("fetch") { "test-result" }

        // Assert
        assertEquals("test-result", result)
        verify(executions, atLeastOnce()).save(any(Execution::class.java))

        val savedExecution = execution
        assertNotNull(savedExecution.state)

        val flow = objectMapper.readValue(savedExecution.state, ExecutionFlow::class.java)
        assertEquals(1, flow.actions.size)
        assertEquals("0", flow.actions[0].id)
        assertEquals("fetch", flow.actions[0].name)
        assertEquals("\"test-result\"", flow.actions[0].result)
    }

    @Test
    fun `test action stores null result`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        ctx.action("process") { null }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals(1, flow.actions.size)
        assertEquals("null", flow.actions[0].result)
    }

    @Test
    fun `test action stores complex object result`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)
        data class TestData(val value: String, val count: Int)

        // Act
        val result = ctx.action("compute") { TestData("test", 42) }

        // Assert
        assertEquals(TestData("test", 42), result)

        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals("""{"value":"test","count":42}""", flow.actions[0].result)
    }

    @Test
    fun `test multiple actions store results`() {
        // Arrange
        val ctx = ExecutionContext("1", objectMapper, executions)

        // Act
        ctx.action("first") { "result1" }
        ctx.action("second") { 42 }
        ctx.action("third") { true }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals(3, flow.actions.size)
        assertEquals("first", flow.actions[0].name)
        assertEquals("\"result1\"", flow.actions[0].result)
        assertEquals("second", flow.actions[1].name)
        assertEquals("42", flow.actions[1].result)
        assertEquals("third", flow.actions[2].name)
        assertEquals("true", flow.actions[2].result)
    }

    @Test
    fun `test constructor restores flow from database`() {
        // Arrange - create existing flow state
        val existingFlow = ExecutionFlow("1")
        existingFlow.actions.add(
            club.kosya.duraexec.internal.ExecutedAction("0").apply {
                name = "existing"
                result = "\"cached\""
            }
        )
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContext("1", objectMapper, executions)
        val result = ctx.action("existing") { "new-execution" }

        // Assert - should re-execute and UPDATE cached result
        assertEquals("new-execution", result)

        val updatedFlow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals(1, updatedFlow.actions.size)
        assertEquals("\"new-execution\"", updatedFlow.actions[0].result)
    }

    @Test
    fun `test constructor creates fresh flow when no state exists`() {
        // Arrange
        execution.state = null

        // Act
        val ctx = ExecutionContext("1", objectMapper, executions)
        ctx.action("first") { "result" }

        // Assert
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals(1, flow.actions.size)
        assertEquals("first", flow.actions[0].name)
    }

    @Test
    fun `test action ID generation works with restored flow`() {
        // Arrange - restored flow
        val existingFlow = ExecutionFlow("1")
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContext("1", objectMapper, executions)
        ctx.action("first") { "a" }
        ctx.action("second") { "b" }

        // Assert - IDs should still be deterministic
        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertEquals("0", flow.actions[0].id)
        assertEquals("1", flow.actions[1].id)
    }
}
