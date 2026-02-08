package club.kosya.lib.lambda.parse

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
        val uuidValue = UUID.randomUUID()

        // Act
        val definition =
            converter.toWorkflowDefinition { testService.doWork(ExecutionContext.Placeholder, uuidValue) }

        // Assert
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doWork", definition.methodName)
        assertEquals(2, definition.parameters.size)
        assertEquals(ExecutionContext::class.java.name, definition.parameters[0].type)

        val param1 = definition.parameters[1]
        // UUID.randomUUID() is a method call, captured as a variable, not a constant
        assertEquals(uuidValue, param1.value)
    }

    @Test
    fun `Method call result as parameter - LocalDateTime now called inline`() {
        // Arrange - no variable, LocalDateTime.now() called directly in lambda

        // Act
        val definition =
            converter.toWorkflowDefinition(
                WorkflowLambda { testService.doWork(ExecutionContext.Placeholder, LocalDateTime.now()) },
            )

        // Assert
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doWork", definition.methodName)
        assertEquals(2, definition.parameters.size)
        assertEquals(ExecutionContext::class.java.name, definition.parameters[0].type)

        val param1 = definition.parameters[1]
        // LocalDateTime.now() is a method call, captured as a variable, not a constant
        assertNotNull(param1.value, "Parameter value should not be null")
    }

    class TestService {
        fun doWork(
            ctx: ExecutionContext,
            input: Any,
        ): String = "Result: $input"
    }
}
