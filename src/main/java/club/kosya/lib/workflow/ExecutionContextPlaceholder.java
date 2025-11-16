package club.kosya.lib.workflow;

import java.time.Duration;
import java.util.function.Supplier;

public final class ExecutionContextPlaceholder implements ExecutionContext {
    
    public ExecutionContextPlaceholder() {}
    
    @Override
    public <R> R action(String name, Supplier<R> lambda) {
        throw new UnsupportedOperationException("Placeholder cannot execute actions");
    }
    
    @Override
    public <R> R await(String name, Supplier<R> lambda) {
        throw new UnsupportedOperationException("Placeholder cannot await actions");
    }
    
    @Override
    public void sleep(Duration duration) {
        throw new UnsupportedOperationException("Placeholder cannot sleep");
    }
}