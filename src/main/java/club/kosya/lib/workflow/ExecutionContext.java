package club.kosya.lib.workflow;

import java.time.Duration;
import java.util.function.Supplier;

public interface ExecutionContext {
    <R> R await(String name, Supplier<R> lambda);
    void sleep(Duration duration);
    
    ExecutionContext Placeholder = new ExecutionContextPlaceholder();
}