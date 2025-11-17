package club.kosya.lib.lambda.parse;


import club.kosya.lib.workflow.ExecutionContext;
import club.kosya.lib.workflow.internal.WorkflowDefinitionConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JavaRecordTest {
    private WorkflowDefinitionConverter converter;
    private TestService testService;

    @BeforeEach
    void setUp() {
        converter = new WorkflowDefinitionConverter();
        testService = new TestService();
    }

    @Test
    void javaRecordFieldAccessorAsParameter() {
        // Arrange
        var request = new Request("testFile", null);

        // Act
        var definition = converter.toWorkflowDefinition(
                () -> testService.doWork(ExecutionContext.Placeholder, request.file())
        );

        // Assert
        assertEquals(TestService.class.getName(), definition.getServiceIdentifier().className());
        assertEquals("doWork", definition.getMethodName());
        assertEquals(2, definition.getParameters().size());
        assertEquals(ExecutionContext.class.getName(), definition.getParameters().get(0).getType());
        assertEquals("testFile", definition.getParameters().get(1).getValue());
    }

    @Test
    void javaRecordFieldAccessorAsParameter_2() {
        // Arrange
        var request = new Request("testFile", new Point(640, 480));

        // Act
        var definition = converter.toWorkflowDefinition(
                () -> testService.doWork(ExecutionContext.Placeholder, request.file(), request.pt())
        );

        // Assert
        assertEquals(TestService.class.getName(), definition.getServiceIdentifier().className());
        assertEquals("doWork", definition.getMethodName());
        assertEquals(3, definition.getParameters().size());
        assertEquals(ExecutionContext.class.getName(), definition.getParameters().get(0).getType());
        assertEquals("testFile", definition.getParameters().get(1).getValue());
        assertEquals(new Point(640, 480), definition.getParameters().get(2).getValue());
    }

    record Request(String file, Point pt) {
    }

    record Point(int x, int y) {
    }

    static class TestService {
        public String doWork(ExecutionContext ctx, Object arg1) {
            return "Result: arg1=" + arg1;
        }

        public String doWork(ExecutionContext ctx, Object arg1, Object arg2) {
            return "Result: arg1=" + arg1 + ", arg2=" + arg2;
        }
    }
}
