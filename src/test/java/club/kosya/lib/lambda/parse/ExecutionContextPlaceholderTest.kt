package club.kosya.lib.lambda.parse

import club.kosya.duraexec.ExecutionContext
import club.kosya.duraexec.WorkflowDefinitionConverter
import club.kosya.lib.lambda.WorkflowLambda
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests ExecutionContext.Placeholder static field access in lambda parameters.
 */
class ExecutionContextPlaceholderTest {

    private lateinit var converter: WorkflowDefinitionConverter
    private lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        converter = WorkflowDefinitionConverter()
        testService = TestService()
    }

    @Test
    fun `ExecutionContext Placeholder - static field access via GETSTATIC`() {
        // Arrange
        val param = "testValue"

        // Act
        val definition = converter.toWorkflowDefinition(
            WorkflowLambda { testService.doWork(ExecutionContext.Placeholder, param) }
        )

        // Assert
        assertEquals(TestService::class.java.name, definition.beanReference.className)
        assertEquals("doWork", definition.methodName)
        assertEquals(2, definition.parameters.size) // Now includes ExecutionContext
        assertNull(definition.parameters[0]) // ExecutionContext.Placeholder resolves to null during parsing
        assertEquals("testValue", definition.parameters[1]) // Actual parameter is second
    }

    class TestService {
        fun doWork(ctx: ExecutionContext, input: Any): String {
            return "Result: $input"
        }
    }
}