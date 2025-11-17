package club.kosya.lib.lambda.parse

import club.kosya.lib.lambda.TypedWorkflowLambda
import club.kosya.lib.lambda.WorkflowLambda
import club.kosya.lib.workflow.ExecutionContext
import club.kosya.lib.workflow.internal.WorkflowDefinitionConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BasicLambdaTest {
    private lateinit var converter: WorkflowDefinitionConverter
    private lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        converter = WorkflowDefinitionConverter()
        testService = TestService()
    }

    @Test
    fun `test instance pattern conversion`() {
        // Arrange
        val param1 = "testValue"
        val ctx = ExecutionContext.Placeholder

        // Act
        val definition =
            converter.toWorkflowDefinition { testService.doWork(ctx, param1) }

        // Assert
        assertNotNull(definition)
        assertNotNull(definition.serviceIdentifier)
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doWork", definition.methodName)

        assertEquals(2, definition.parameters.size)
        assertEquals(ExecutionContext::class.java.name, definition.parameters[0].type)
        assertEquals("ctx", definition.parameters[0].name)
        assertEquals("input", definition.parameters[1].name)
        assertEquals(param1, definition.parameters[1].value)
    }

    @Test
    fun `test instance pattern with multiple params`() {
        // Arrange
        val param1 = "value1"
        val param2 = 42
        val ctx = ExecutionContext.Placeholder

        // Act
        val definition =
            converter.toWorkflowDefinition { testService.doComplexWork(ctx, param1, param2) }

        // Assert
        assertNotNull(definition)
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doComplexWork", definition.methodName)
        assertEquals(3, definition.parameters.size)
        assertEquals(ExecutionContext::class.java.name, definition.parameters[0].type)
        assertEquals(param1, definition.parameters[1].value)
        assertEquals(param2, definition.parameters[2].value)
    }

    @Test
    fun `test typed pattern conversion`() {
        // Arrange
        val param1 = "testValue"
        val ctx = ExecutionContext.Placeholder

        // Act
        val definition =
            converter.toWorkflowDefinition(
                TypedWorkflowLambda<TestService> { it.doWork(ctx, param1) },
            )

        // Assert
        assertNotNull(definition)
        assertNotNull(definition.serviceIdentifier)
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doWork", definition.methodName)

        assertEquals(2, definition.parameters.size)
        assertEquals(ExecutionContext::class.java.name, definition.parameters[0].type)
        assertEquals("ctx", definition.parameters[0].name) // Real parameter name from method signature
        assertEquals("input", definition.parameters[1].name) // Real parameter name from method signature
        assertEquals(param1, definition.parameters[1].value)
    }

    @Test
    fun `test typed pattern with multiple params`() {
        // Arrange
        val param1 = "value1"
        val param2 = 42
        val ctx = ExecutionContext.Placeholder

        // Act
        val definition =
            converter.toWorkflowDefinition(
                TypedWorkflowLambda<TestService> { it.doComplexWork(ctx, param1, param2) },
            )

        // Assert
        assertNotNull(definition)
        assertEquals(TestService::class.java.name, definition.serviceIdentifier.className)
        assertEquals("doComplexWork", definition.methodName)
        assertEquals(3, definition.parameters.size)
        assertEquals(ExecutionContext::class.java.name, definition.parameters[0].type)
        assertEquals(param1, definition.parameters[1].value)
        assertEquals(param2, definition.parameters[2].value)
    }

    @Test
    fun `test both patterns produce same result`() {
        // Arrange
        val ctx = ExecutionContext.Placeholder

        // Act
        val instanceDef =
            converter.toWorkflowDefinition(
                WorkflowLambda { testService.doWork(ctx, "test") },
            )
        val typedDef =
            converter.toWorkflowDefinition(
                TypedWorkflowLambda<TestService> { it.doWork(ctx, "test") },
            )

        // Assert - Both resolve by type
        assertEquals(TestService::class.java.name, instanceDef.serviceIdentifier.className)
        assertEquals(TestService::class.java.name, typedDef.serviceIdentifier.className)
        assertEquals(instanceDef.methodName, typedDef.methodName)
        assertEquals(instanceDef.parameters, typedDef.parameters)
    }

    /**
     * Test service used for lambda testing
     */
    class TestService {
        fun doWork(
            ctx: ExecutionContext,
            input: String,
        ): String = "Result: $input"

        fun doComplexWork(
            ctx: ExecutionContext,
            param1: String,
            param2: Int,
        ): String = "Result: $param1, $param2"
    }
}
