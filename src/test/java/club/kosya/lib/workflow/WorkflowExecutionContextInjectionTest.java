package club.kosya.lib.workflow;

import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl;
import club.kosya.lib.executionengine.internal.ExecutionContextImpl;
import club.kosya.lib.executionengine.internal.ExecutionsRepository;
import club.kosya.lib.workflow.internal.WorkflowReconstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

class WorkflowExecutionContextInjectionTest {
    private WorkflowReconstructor workflowReconstructor;
    private TestWorkflowService testService;

    @BeforeEach
    void setUp() {
        testService = new TestWorkflowService();

        ServiceInstanceProvider serviceInstanceProvider = serviceIdentifier -> {
            if (serviceIdentifier.className().equals(TestWorkflowService.class.getName())) {
                return testService;
            } else {
                throw new IllegalArgumentException("Unknown service: " + serviceIdentifier.className());
            }
        };

        var objectDeserializer = new ObjectDeserializerImpl(new ObjectMapper());
        workflowReconstructor = new WorkflowReconstructor(serviceInstanceProvider, objectDeserializer);
    }

    @Test
    void reconstructAndExecute_shouldInjectRealExecutionContextNotNull() {
        // Arrange
        var definition = new WorkflowDefinition();
        definition.setServiceIdentifier(new ServiceIdentifier(TestWorkflowService.class.getName()));
        definition.setMethodName("processVideo");

        WorkflowParameter[] parameters = new WorkflowParameter[]{
                new WorkflowParameter() {{
                    setName("ctx");
                    setType(ExecutionContext.class.getName());
                    setValue(null);
                }},
                new WorkflowParameter() {{
                    setName("videoFile");
                    setType("java.lang.String");
                    setValue("test.mp4");
                }}
        };

        definition.setParameters(Arrays.asList(parameters));

        var objectMapper = new ObjectMapper();
        var executionContext =
                new ExecutionContextImpl("123", objectMapper, Mockito.mock(ExecutionsRepository.class), new ObjectDeserializerImpl(objectMapper));

        // Act - WorkflowReconstructor should inject real ExecutionContext despite null value in definition
        Object result = workflowReconstructor.reconstructAndExecute(definition, () -> executionContext);

        // Assert - this should pass with the fix
        Assertions.assertNotNull(result, "Result should not be null");
        Assertions.assertNotNull(testService.getLastExecutionContext(), "ExecutionContext should not be null");
        Assertions.assertTrue(testService.getLastExecutionContext() instanceof ExecutionContextImpl,
                "Should receive ExecutionContextImplementation instance");
        Assertions.assertEquals("Processed test.mp4", result, "Should process video correctly");
    }

    static class TestWorkflowService {
        private club.kosya.lib.workflow.ExecutionContext lastExecutionContext;

        public String processVideo(club.kosya.lib.workflow.ExecutionContext ctx, String videoFile) {
            this.lastExecutionContext = ctx;

            // This would fail if ctx is null, but should never happen with the fix
            if (ctx == null) {
                throw new IllegalArgumentException("ExecutionContext is null!");
            }

            return "Processed " + videoFile;
        }

        public club.kosya.lib.workflow.ExecutionContext getLastExecutionContext() {
            return lastExecutionContext;
        }
    }
}
