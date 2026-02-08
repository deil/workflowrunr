package club.kosya.lib.executionengine.internal;

public class WorkflowSuspendedException extends RuntimeException {
    public WorkflowSuspendedException(String reason) {
        super(reason);
    }
}
