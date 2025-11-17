package club.kosya.lib.workflow.internal;

import club.kosya.lib.workflow.ExecutionContext;
import club.kosya.lib.workflow.ServiceIdentifier;
import club.kosya.lib.workflow.WorkflowDefinition;
import club.kosya.lib.workflow.WorkflowParameter;
import club.kosya.lib.workflow.ServiceInstanceProvider;
import club.kosya.lib.executionengine.internal.ExecutionContextImplementation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * Unit test to reproduce and verify fix for ExecutionContext null injection issue.
 * RED phase: Demonstrates the bug where ExecutionContext is null
 * GREEN phase: Verifies the fix works correctly
 */
class WorkflowExecutionContextInjectionTest {
    private WorkflowReconstructor workflowReconstructor;
    private TestWorkflowService testService;

    @BeforeEach
    void setUp() {
        testService = new TestWorkflowService();
        
        // Create ServiceInstanceProvider that returns our test service
        ServiceInstanceProvider serviceInstanceProvider = serviceIdentifier -> {
            if (serviceIdentifier.className().equals(TestWorkflowService.class.getName())) {
                return testService;
            } else {
                throw new IllegalArgumentException("Unknown service: " + serviceIdentifier.className());
            }
        };
        
        workflowReconstructor = new WorkflowReconstructor(serviceInstanceProvider);
    }

    @Test
    void reconstructAndExecute_shouldInjectRealExecutionContextNotNull() {
        // Arrange - create workflow definition with ExecutionContext parameter that has null value
        // This simulates the bug where ExecutionContext.Placeholder becomes null during serialization
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setServiceIdentifier(new ServiceIdentifier(TestWorkflowService.class.getName()));
        definition.setMethodName("processVideo");
        
        WorkflowParameter[] parameters = new WorkflowParameter[] {
            new WorkflowParameter() {{
                setName("ctx");
                setType(ExecutionContext.class.getName());
                setValue(null); // ExecutionContext.Placeholder becomes null during serialization
            }},
            new WorkflowParameter() {{
                setName("videoFile");
                setType("java.lang.String");
                setValue("test.mp4");
            }}
        };
        
        definition.setParameters(Arrays.asList(parameters));

        ExecutionContextImplementation executionContext = 
            new ExecutionContextImplementation("123", new ObjectMapper(), Mockito.mock(club.kosya.lib.executionengine.ExecutionsRepository.class));

        // Act - WorkflowReconstructor should inject real ExecutionContext despite null value in definition
        Object result = workflowReconstructor.reconstructAndExecute(definition, () -> executionContext);

        // Assert - this should pass with the fix
        Assertions.assertNotNull(result, "Result should not be null");
        Assertions.assertNotNull(testService.getLastExecutionContext(), "ExecutionContext should not be null");
        Assertions.assertTrue(testService.getLastExecutionContext() instanceof ExecutionContextImplementation, 
                          "Should receive ExecutionContextImplementation instance");
        Assertions.assertEquals("Processed test.mp4", result, "Should process video correctly");
    }

    /**
     * Test service that simulates TranscribeVideoWorkflow behavior
     */
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