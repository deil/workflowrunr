package club.kosya.lib.lambda.parse

import club.kosya.lib.executionengine.internal.ExecutionContextImplementation
import club.kosya.lib.lambda.WorkflowLambda
import club.kosya.lib.workflow.ExecutionContext
import club.kosya.lib.workflow.internal.WorkflowDefinitionConverter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Tests method call results as lambda parameters.
 */
class MethodCallParameterTest {
    private lateinit var converter: WorkflowDefinitionConverter
    private lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        converter = WorkflowDefinitionConverter()
        testService = TestService()
    }

    @Test
    fun `Method call result as parameter - UUID randomUUID called inline`() {
        // Arrange - no variable, UUID.randomUUID() called directly in lambda

        // Act
        val definition =
            converter.toWorkflowDefinition(
                WorkflowLambda { testService.doWork(ExecutionContext.Placeholder, UUID.randomUUID()) },
            )

        // Assert
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doWork", definition.methodName)
        assertEquals(2, definition.parameters.size) // Now includes ExecutionContext
        assertNull(definition.parameters[0]) // ExecutionContext.Placeholder resolves to null during parsing
        assertTrue(definition.parameters[1] is UUID, "Parameter should be UUID instance") // UUID is second
    }

    @Test
    fun `Method call result as parameter - LocalDateTime now called inline`() {
        // Arrange - no variable, LocalDateTime.now() called directly in lambda

        // Act
        val definition =
            converter.toWorkflowDefinition {
                testService.doWork(ExecutionContext.Placeholder, LocalDateTime.now())
            }

        // Assert
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doWork", definition.methodName)
        assertEquals(2, definition.parameters.size) // Now includes ExecutionContext
        assertNull(definition.parameters[0]) // ExecutionContext.Placeholder resolves to null during parsing
        assertTrue(definition.parameters[1] is LocalDateTime, "Parameter should be LocalDateTime instance") // LocalDateTime is second
    }

    class TestService {
        fun doWork(
            ctx: ExecutionContext,
            input: Any,
        ): String = "Result: $input"
    }
}
