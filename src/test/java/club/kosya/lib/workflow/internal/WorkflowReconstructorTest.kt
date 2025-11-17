package club.kosya.lib.workflow.internal

import club.kosya.lib.executionengine.internal.ExecutionContextImplementation
import club.kosya.lib.workflow.ServiceIdentifier
import club.kosya.lib.workflow.ServiceInstanceProvider
import club.kosya.lib.workflow.WorkflowDefinition
import club.kosya.lib.workflow.WorkflowParameter
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * Unit test for WorkflowReconstructor focusing on ExecutionContext injection behavior.
 */
class WorkflowReconstructorTest {
    private lateinit var workflowReconstructor: WorkflowReconstructor
    private lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        testService = TestService()

        // Create ServiceInstanceProvider that returns our test service
        val serviceInstanceProvider =
            ServiceInstanceProvider { serviceIdentifier ->
                if (serviceIdentifier.className == TestService::class.java.name) {
                    testService
                } else {
                    throw IllegalArgumentException("Unknown service: ${serviceIdentifier.className}")
                }
            }

        workflowReconstructor = WorkflowReconstructor(serviceInstanceProvider)
    }

    @Test
    fun `reconstructAndExecute should filter out null ExecutionContext placeholder`() {
        // Arrange - create workflow definition with ExecutionContext placeholder
        val definition =
            WorkflowDefinition().apply {
                serviceIdentifier = ServiceIdentifier(TestService::class.java.name)
                methodName = "doWork"
                parameters =
                    listOf(
                        WorkflowParameter().apply {
                            name = "param0"
                            type = club.kosya.lib.workflow.ExecutionContext::class.java.name
                            value = null
                        },
                        WorkflowParameter().apply {
                            name = "param1"
                            value = "testParam"
                            type = "java.lang.String"
                            type = "java.lang.String"
                        },
                    )
            }

        val executionContext =
            ExecutionContextImplementation("123", ObjectMapper(), mock(club.kosya.lib.executionengine.ExecutionsRepository::class.java))

        // Act
        val result = workflowReconstructor.reconstructAndExecute(definition) { executionContext }

        // Assert
        assertEquals("Result: testParam", result)
        assertNotNull(testService.lastExecutionContext, "ExecutionContext should not be null")
        assertTrue(
            testService.lastExecutionContext!!
                .javaClass.name
                .contains("ExecutionContextImplementation"),
            "Should receive ExecutionContextImplementation instance",
        )
    }

    @Test
    fun `reconstructAndExecute should preserve parameter order when filtering nulls`() {
        // Arrange - parameters with ExecutionContext placeholder mixed with real parameters
        val definition =
            WorkflowDefinition().apply {
                serviceIdentifier = ServiceIdentifier(TestService::class.java.name)
                methodName = "orderedWork"
                parameters =
                    listOf(
                        WorkflowParameter().apply {
                            name = "param0"
                            type = club.kosya.lib.workflow.ExecutionContext::class.java.name
                            value = null
                        },
                        WorkflowParameter().apply {
                            name = "param1"
                            value = "first"
                            type = "java.lang.String"
                            type = "java.lang.String"
                        },
                        WorkflowParameter().apply {
                            name = "param2"
                            value = 999
                            type = "java.lang.Integer"
                            type = "java.lang.String"
                        },
                        WorkflowParameter().apply {
                            name = "param3"
                            value = "second"
                            type = "java.lang.String"
                            type = "java.lang.String"
                        },
                    )
            }

        val executionContext =
            ExecutionContextImplementation("999", ObjectMapper(), mock(club.kosya.lib.executionengine.ExecutionsRepository::class.java))

        // Act
        val result = workflowReconstructor.reconstructAndExecute(definition) { executionContext }

        // Assert
        assertEquals("first-999-second", result)
        assertNotNull(testService.lastExecutionContext)
    }

    class TestService {
        var lastExecutionContext: club.kosya.lib.workflow.ExecutionContext? = null

        fun doWork(
            ctx: club.kosya.lib.workflow.ExecutionContext,
            input: Any,
        ): String {
            lastExecutionContext = ctx
            return "Result: $input"
        }

        fun orderedWork(
            ctx: club.kosya.lib.workflow.ExecutionContext,
            text1: String,
            number: Int,
            text2: String,
        ): String {
            lastExecutionContext = ctx
            return "$text1-$number-$text2"
        }
    }
}
