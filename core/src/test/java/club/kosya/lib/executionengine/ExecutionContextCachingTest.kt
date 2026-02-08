package club.kosya.lib.executionengine

import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl
import club.kosya.lib.executionengine.internal.ExecutedAction
import club.kosya.lib.executionengine.internal.Execution
import club.kosya.lib.executionengine.internal.ExecutionContextImpl
import club.kosya.lib.executionengine.internal.ExecutionFlow
import club.kosya.lib.executionengine.internal.ExecutionsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.Optional

class ExecutionContextCachingTest {
    private lateinit var objectMapper: ObjectMapper
    private lateinit var executions: ExecutionsRepository
    private lateinit var execution: Execution
    private lateinit var deserializer: ObjectDeserializerImpl

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        executions = Mockito.mock(ExecutionsRepository::class.java)
        deserializer = ObjectDeserializerImpl(objectMapper)

        execution =
            Execution().apply {
                id = 1L
                status = ExecutionStatus.Running
                queuedAt = LocalDateTime.now()
                definition = byteArrayOf()
                params = "{}"
            }

        Mockito.`when`(executions.findById(1L)).thenReturn(Optional.of(execution))
        Mockito.`when`(executions.save(ArgumentMatchers.any(Execution::class.java))).thenReturn(execution)
    }

    @Test
    fun `should return cached result when action was previously completed`() {
        // Arrange
        val existingFlow = ExecutionFlow("1")
        existingFlow.actions.add(
            ExecutedAction("0").apply {
                name = "fetch-data"
                result = "\"cached-result\""
                resultType = "java.lang.String"
                completed = true
            },
        )
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val result = ctx.action("fetch-data") { "should-not-execute" }

        // Assert
        Assertions.assertEquals("cached-result", result)

        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        Assertions.assertEquals(1, flow.actions.size)
        Assertions.assertEquals("\"cached-result\"", flow.actions[0].result)
    }

    @Test
    fun `should execute action and cache result when not previously completed`() {
        // Arrange
        val existingFlow = ExecutionFlow("1")
        existingFlow.actions.add(
            ExecutedAction("0").apply {
                name = "process-data"
                result = null
                resultType = null
                completed = false
            },
        )
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val result = ctx.action("process-data") { "new-result" }

        // Assert
        Assertions.assertEquals("new-result", result)

        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        Assertions.assertEquals(1, flow.actions.size)
        Assertions.assertEquals("\"new-result\"", flow.actions[0].result)
        Assertions.assertEquals("java.lang.String", flow.actions[0].resultType)
    }

    @Test
    fun `should execute and cache when action does not exist in flow`() {
        // Arrange
        val existingFlow = ExecutionFlow("1")
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val result =
            ctx.action("new-action") {
                Path
                    .of("/tmp/test.txt")
            }

        // Assert
        Assertions.assertEquals("UnixPath", result::class.java.simpleName)

        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        Assertions.assertEquals(1, flow.actions.size)
        Assertions.assertEquals("new-action", flow.actions[0].name)
        Assertions.assertEquals("\"file:///tmp/test.txt\"", flow.actions[0].result)
        Assertions.assertEquals("sun.nio.fs.UnixPath", flow.actions[0].resultType)
    }

    @Test
    fun `should handle null cached results`() {
        // Arrange
        val existingFlow = ExecutionFlow("1")
        existingFlow.actions.add(
            ExecutedAction("0").apply {
                name = "null-action"
                result = "null"
                resultType = null
                completed = true
            },
        )
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val result = ctx.action("null-action") { "should-not-execute" }

        // Assert
        Assertions.assertNull(result)
    }

    @Test
    fun `should handle complex object cached results`() {
        // Arrange
        data class TestData(
            val value: String,
            val count: Int,
        )

        val existingFlow = ExecutionFlow("1")
        existingFlow.actions.add(
            ExecutedAction("0").apply {
                name = "complex-action"
                result = "\"cached\""
                resultType = "java.lang.String"
                completed = true
            },
        )
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val result = ctx.action("complex-action") { "new-result" }

        // Assert
        Assertions.assertEquals("cached", result)
    }

    @Test
    fun `should handle multiple actions with mixed completion states`() {
        // Arrange
        val existingFlow = ExecutionFlow("1")
        existingFlow.actions.add(
            ExecutedAction("0").apply {
                name = "completed-action"
                result = "\"done\""
                resultType = "java.lang.String"
                completed = true
            },
        )
        existingFlow.actions.add(
            ExecutedAction("1").apply {
                name = "incomplete-action"
                result = null
                resultType = null
                completed = false
            },
        )
        existingFlow.actions.add(
            ExecutedAction("1").apply {
                name = "incomplete-action"
                result = null
                resultType = null
                completed = false
            },
        )
        execution.state = objectMapper.writeValueAsString(existingFlow)

        // Act
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val result1 = ctx.action("completed-action") { "should-not-execute" }
        val result2 = ctx.action("incomplete-action") { "new-result" }

        // Assert
        Assertions.assertEquals("done", result1)
        Assertions.assertEquals("new-result", result2)

        val flow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        Assertions.assertEquals(3, flow.actions.size)
        Assertions.assertEquals("\"done\"", flow.actions[0].result)
        Assertions.assertEquals("\"new-result\"", flow.actions[1].result)
    }
}
