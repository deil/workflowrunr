package club.kosya.duraexec.spring;

import club.kosya.duraexec.workflows.SleepWorkflow;
import club.kosya.duraexec.workflows.TranscribeVideoWorkflow;
import club.kosya.lib.workflow.ExecutionContext;
import club.kosya.lib.workflow.Workflow;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class DummyController {
    private final Workflow workflow;
    private final TranscribeVideoWorkflow transcribeVideoWorkflow;
    private final SleepWorkflow sleepWorkflow;

    @Transactional
    @PostMapping("/typed")
    public String runExampleWorkflowIocPattern(@RequestBody RunExampleWorkflowRequest body) {
        var fileName = body.file();
        var executionId =
                workflow.<TranscribeVideoWorkflow>run(x -> x.processVideo(ExecutionContext.Placeholder, fileName));
        return Long.toString(executionId);
    }

    @Transactional
    @PostMapping
    public String runExampleWorkflow(@RequestBody RunExampleWorkflowRequest body) {
        var executionId =
                workflow.run(() -> transcribeVideoWorkflow.processVideo(ExecutionContext.Placeholder, body.file()));
        return Long.toString(executionId);
    }

    @Transactional
    @PostMapping("/sleep")
    public String runSleepWorkflow() {
        var executionId = workflow.run(() -> sleepWorkflow.sleepDemo(ExecutionContext.Placeholder));
        return Long.toString(executionId);
    }

    @Transactional
    @PostMapping("/cancel/{executionId}")
    public String cancelWorkflow(@PathVariable long executionId) {
        workflow.cancel(executionId);
        return "Workflow " + executionId + " cancelled successfully";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RunExampleWorkflowRequest(String file) {}
}
