package club.kosya.lib.lambda.parse

import club.kosya.duraexec.ExecutionContext
import club.kosya.duraexec.WorkflowDefinitionConverter
import club.kosya.lib.lambda.TypedWorkflowLambda
import club.kosya.lib.lambda.WorkflowLambda
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests basic lambda parsing scenarios.
 */
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
        val definition = converter.toWorkflowDefinition(
            WorkflowLambda { testService.doWork(ctx, param1) }
        )

        // Assert
        assertNotNull(definition)
        assertNotNull(definition.beanReference)
        assertEquals(TestService::class.java.name, definition.beanReference.className)
        assertEquals("doWork", definition.methodName)
        assertNotNull(definition.methodDescriptor)
        assertEquals(2, definition.parameters.size) // Now includes ExecutionContext
        assertEquals(ctx, definition.parameters[0]) // ExecutionContext is first
        assertEquals(param1, definition.parameters[1]) // Actual parameter is second
    }

    @Test
    fun `test instance pattern with multiple params`() {
        // Arrange
        val param1 = "value1"
        val param2 = 42
        val ctx = ExecutionContext.Placeholder

        // Act
        val definition = converter.toWorkflowDefinition(
            WorkflowLambda { testService.doComplexWork(ctx, param1, param2) }
        )

        // Assert
        assertNotNull(definition)
        assertEquals(TestService::class.java.name, definition.beanReference.className)
        assertEquals("doComplexWork", definition.methodName)
        assertEquals(3, definition.parameters.size) // Now includes ExecutionContext
        assertEquals(ctx, definition.parameters[0]) // ExecutionContext is first
        assertEquals(param1, definition.parameters[1]) // First actual parameter
        assertEquals(param2, definition.parameters[2]) // Second actual parameter
    }

    @Test
    fun `test typed pattern conversion`() {
        // Arrange
        val param1 = "testValue"
        val ctx = ExecutionContext.Placeholder

        // Act
        val definition = converter.toWorkflowDefinition(
            TypedWorkflowLambda<TestService> { it.doWork(ctx, param1) }
        )

        // Assert
        assertNotNull(definition)
        assertNotNull(definition.beanReference)
        assertEquals(TestService::class.java.name, definition.beanReference.className)
        assertEquals("doWork", definition.methodName)
        assertNotNull(definition.methodDescriptor)
        assertEquals(2, definition.parameters.size) // Now includes ExecutionContext
        assertEquals(ctx, definition.parameters[0]) // ExecutionContext is first
        assertEquals(param1, definition.parameters[1]) // Actual parameter is second
    }

    @Test
    fun `test typed pattern with multiple params`() {
        // Arrange
        val param1 = "value1"
        val param2 = 42
        val ctx = ExecutionContext.Placeholder

        // Act
        val definition = converter.toWorkflowDefinition(
            TypedWorkflowLambda<TestService> { it.doComplexWork(ctx, param1, param2) }
        )

        // Assert
        assertNotNull(definition)
        assertEquals(TestService::class.java.name, definition.beanReference.className)
        assertEquals("doComplexWork", definition.methodName)
        assertEquals(3, definition.parameters.size) // Now includes ExecutionContext
        assertEquals(ctx, definition.parameters[0]) // ExecutionContext is first
        assertEquals(param1, definition.parameters[1]) // First actual parameter
        assertEquals(param2, definition.parameters[2]) // Second actual parameter
    }

    @Test
    fun `test both patterns produce same result`() {
        // Arrange
        val ctx = ExecutionContext.Placeholder

        // Act
        val instanceDef = converter.toWorkflowDefinition(
            WorkflowLambda { testService.doWork(ctx, "test") }
        )
        val typedDef = converter.toWorkflowDefinition(
            TypedWorkflowLambda<TestService> { it.doWork(ctx, "test") }
        )

        // Assert - Both resolve by type
        assertEquals(TestService::class.java.name, instanceDef.beanReference.className)
        assertEquals(TestService::class.java.name, typedDef.beanReference.className)
        assertEquals(instanceDef.methodName, typedDef.methodName)
        assertEquals(instanceDef.parameters, typedDef.parameters)
    }

    /**
     * Test service used for lambda testing
     */
    class TestService {
        fun doWork(ctx: ExecutionContext, input: String): String {
            return "Result: $input"
        }

        fun doComplexWork(ctx: ExecutionContext, param1: String, param2: Int): String {
            return "Result: $param1, $param2"
        }
    }
}
