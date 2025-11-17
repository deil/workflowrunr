package club.kosya.lib.lambda.parse

import club.kosya.lib.lambda.WorkflowLambda
import club.kosya.lib.workflow.ExecutionContext
import club.kosya.lib.workflow.internal.WorkflowDefinitionConverter
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
        val definition =
            converter.toWorkflowDefinition(
                WorkflowLambda { testService.doWork(ExecutionContext.Placeholder, param) },
            )

        // Assert
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doWork", definition.methodName)
        assertEquals(2, definition.parameters.size)
        assertEquals(ExecutionContext::class.java.name, definition.parameters[0].type)
        assertEquals("testValue", definition.parameters[1].value)
    }

    class TestService {
        fun doWork(
            ctx: ExecutionContext,
            input: Any,
        ): String = "Result: $input"
    }
}
