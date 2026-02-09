package club.kosya.lib.workflow;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.Supplier;

public interface ExecutionContext {
    <R> R await(String name, Supplier<R> lambda);

    void sleep(Duration duration);

    void sleepUntil(Instant resumeAt);

    ExecutionContext Placeholder = new ExecutionContextPlaceholder();
}
