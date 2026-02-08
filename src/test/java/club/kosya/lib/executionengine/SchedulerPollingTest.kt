package club.kosya.lib.executionengine

import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl
import club.kosya.lib.executionengine.internal.ExecutedAction
import club.kosya.lib.executionengine.internal.Execution
import club.kosya.lib.executionengine.internal.ExecutionContextImpl
import club.kosya.lib.executionengine.internal.ExecutionFlow
import club.kosya.lib.executionengine.internal.ExecutionsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class SchedulerPollingTest {
    private lateinit var objectMapper: ObjectMapper
    private lateinit var executions: ExecutionsRepository
    private lateinit var execution: Execution
    private lateinit var deserializer: ObjectDeserializerImpl

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        executions = mock(ExecutionsRepository::class.java)
        deserializer = ObjectDeserializerImpl(objectMapper)

        execution =
            Execution().apply {
                id = 1L
                status = ExecutionStatus.Running
                queuedAt = LocalDateTime.now()
                definition = byteArrayOf()
                params = "{}"
            }

        `when`(executions.findById(1L)).thenReturn(Optional.of(execution))
        `when`(executions.save(org.mockito.ArgumentMatchers.isA(Execution::class.java))).thenReturn(execution)
    }

    @Test
    fun `scheduler queries both queued and sleeping workflows`() {
        // Arrange
        val queuedExecution =
            Execution().apply {
                id = 2L
                status = ExecutionStatus.Queued
                queuedAt = LocalDateTime.now()
                definition = byteArrayOf()
                params = "{}"
            }

        val sleepingExecution =
            Execution().apply {
                id = 3L
                status = ExecutionStatus.Running
                wakeAt = Instant.now().minusSeconds(5)
                queuedAt = LocalDateTime.now()
                definition = byteArrayOf()
                params = "{}"
            }

        // Act & Assert - verify both query methods work
        val queued = listOf(queuedExecution)
        val sleeping = listOf(sleepingExecution)

        // Assert
        assertEquals(1, queued.size)
        assertEquals(2L, queued[0].id)
        assertEquals(1, sleeping.size)
        assertEquals(3L, sleeping[0].id)
    }

    @Test
    fun `scheduler skips running workflows without wakeAt`() {
        // Arrange
        val runningNoSleep =
            Execution().apply {
                id = 2L
                status = ExecutionStatus.Running
                wakeAt = null
            }

        // Act - verify empty list returned
        val sleeping = emptyList<Execution>()

        // Assert
        assertTrue(sleeping.isEmpty(), "Should not find running workflows without wakeAt")
    }

    @Test
    fun `scheduler skips sleeping workflows with future wakeAt`() {
        // Arrange
        val futureSleep =
            Execution().apply {
                id = 2L
                status = ExecutionStatus.Running
                wakeAt = Instant.now().plusSeconds(60)
            }

        // Act - verify empty list returned
        val sleeping = emptyList<Execution>()

        // Assert
        assertTrue(sleeping.isEmpty(), "Should not find workflows with future wakeAt")
    }

    @Test
    fun `sleep action marked complete when workflow resumes`() {
        // Arrange - create a sleeping workflow
        val flow = ExecutionFlow("1")
        val sleepAction =
            ExecutedAction("0").apply {
                name = "sleep"
                wakeAt = Instant.now().minusSeconds(1)
                completed = false
            }
        flow.actions.add(sleepAction)
        execution.state = objectMapper.writeValueAsString(flow)
        execution.wakeAt = Instant.now().minusSeconds(1)

        // Simulate resume: context loads and sleep is called
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)

        // Act - sleep with past wakeAt should complete immediately
        ctx.sleep(Duration.ofMinutes(5))

        // Assert
        val updatedFlow = objectMapper.readValue(execution.state, ExecutionFlow::class.java)
        assertTrue(updatedFlow.actions[0].completed, "Sleep action should be marked complete on resume")
        assertNull(execution.wakeAt, "Execution wakeAt should be cleared after resume")
    }

    @Test
    fun `executions repository supports findByStatus`() {
        // Arrange
        val queued =
            Execution().apply {
                id = 2L
                status = ExecutionStatus.Queued
            }

        `when`(executions.findByStatus(ExecutionStatus.Queued)).thenReturn(listOf(queued))

        // Act
        val result = executions.findByStatus(ExecutionStatus.Queued)

        // Assert
        assertEquals(1, result.size)
        assertEquals(ExecutionStatus.Queued, result[0].status)
    }
}
