package club.kosya.lib.executionengine.internal;

public class WorkflowCanceledException extends RuntimeException {
    public WorkflowCanceledException(String message) {
        super(message);
    }
}
