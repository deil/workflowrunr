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
        assertNull(definition.getParameters().get(0));
        assertEquals("testFile", definition.getParameters().get(1));
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
        assertEquals(3, definition.getParameters().size()); // Now includes ExecutionContext
        assertNull(definition.getParameters().get(0)); // ExecutionContext.Placeholder resolves to null during parsing
        assertEquals("testFile", definition.getParameters().get(1)); // First actual parameter
        assertEquals(new Point(640, 480), definition.getParameters().get(2)); // Second actual parameter
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
