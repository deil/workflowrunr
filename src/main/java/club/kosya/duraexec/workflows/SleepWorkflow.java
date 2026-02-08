package club.kosya.duraexec.workflows;

import club.kosya.lib.workflow.ExecutionContext;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SleepWorkflow {

    public String sleepDemo(ExecutionContext ctx) {
        log.info("Starting sleep demo workflow");

        log.info("Sleeping for 1 second...");
        ctx.sleep(Duration.ofSeconds(10));
        log.info("Woke up after 1 second");

        log.info("Sleeping for 5 seconds...");
        ctx.sleep(Duration.ofSeconds(20));
        log.info("Woke up after 5 seconds");

        log.info("Sleeping for 15 seconds...");
        ctx.sleep(Duration.ofSeconds(30));
        log.info("Woke up after 15 seconds");

        log.info("Sleep demo workflow completed");
        return "Slept for 1s + 5s + 15s = 21s total";
    }
}
