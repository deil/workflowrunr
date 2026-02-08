package club.kosya.lib.workflow

import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl
import club.kosya.lib.executionengine.internal.ExecutionContextImpl
import club.kosya.lib.executionengine.internal.ExecutionsRepository
import club.kosya.lib.workflow.internal.WorkflowReconstructor
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class WorkflowReconstructorTest {
    private lateinit var workflowReconstructor: WorkflowReconstructor
    private lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        testService = TestService()

        val serviceInstanceProvider =
            ServiceInstanceProvider { serviceIdentifier ->
                if (serviceIdentifier.className == TestService::class.java.name) {
                    testService
                } else {
                    throw IllegalArgumentException("Unknown service: ${serviceIdentifier.className}")
                }
            }

        val objectDeserializer = ObjectDeserializerImpl(ObjectMapper())
        workflowReconstructor = WorkflowReconstructor(serviceInstanceProvider, objectDeserializer)
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
                            type = ExecutionContext::class.java.name
                            value = null
                        },
                        WorkflowParameter().apply {
                            name = "param1"
                            value = "testParam"
                            type = "java.lang.String"
                        },
                    )
            }

        val objectMapper = ObjectMapper()
        val executionContext =
            ExecutionContextImpl("123", objectMapper, Mockito.mock(ExecutionsRepository::class.java), ObjectDeserializerImpl(objectMapper))

        // Act
        val result = workflowReconstructor.reconstructAndExecute(definition) { executionContext }

        // Assert
        Assertions.assertEquals("Result: testParam", result)
        Assertions.assertNotNull(testService.lastExecutionContext, "ExecutionContext should not be null")
        Assertions.assertTrue(
            testService.lastExecutionContext!!
                .javaClass.name == ExecutionContextImpl::class.java.name,
            "Should receive ExecutionContextImpl instance",
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
                            type = ExecutionContext::class.java.name
                            value = null
                        },
                        WorkflowParameter().apply {
                            name = "param1"
                            value = "first"
                            type = "java.lang.String"
                        },
                        WorkflowParameter().apply {
                            name = "param2"
                            value = "999"
                            type = "java.lang.Integer"
                        },
                        WorkflowParameter().apply {
                            name = "param3"
                            value = "second"
                            type = "java.lang.String"
                        },
                    )
            }

        val objectMapper = ObjectMapper()
        val executionContext =
            ExecutionContextImpl("999", objectMapper, Mockito.mock(ExecutionsRepository::class.java), ObjectDeserializerImpl(objectMapper))

        // Act
        val result = workflowReconstructor.reconstructAndExecute(definition) { executionContext }

        // Assert
        Assertions.assertEquals("first-999-second", result)
        Assertions.assertNotNull(testService.lastExecutionContext)
    }

    class TestService {
        var lastExecutionContext: ExecutionContext? = null

        fun doWork(
            ctx: ExecutionContext,
            input: Any,
        ): String {
            lastExecutionContext = ctx
            return "Result: $input"
        }

        fun orderedWork(
            ctx: ExecutionContext,
            text1: String,
            number: Int,
            text2: String,
        ): String {
            lastExecutionContext = ctx
            return "$text1-$number-$text2"
        }
    }
}
