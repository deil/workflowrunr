package club.kosya.lib.workflow;

import club.kosya.lib.executionengine.internal.Execution;
import club.kosya.lib.executionengine.ExecutionStatus;
import club.kosya.lib.executionengine.internal.ExecutionsRepository;
import club.kosya.lib.lambda.TypedWorkflowLambda;
import club.kosya.lib.lambda.WorkflowLambda;
import club.kosya.lib.workflow.internal.WorkflowDefinitionConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class Workflow {
    private final ExecutionsRepository executions;
    private final ObjectMapper objectMapper;
    private final WorkflowDefinitionConverter converter;

    @SneakyThrows
    @Transactional
    public long run(WorkflowLambda workflow) {
        var definition = converter.toWorkflowDefinition(workflow);
        return persistAndQueue(definition);
    }

    @SneakyThrows
    @Transactional
    public <T> long run(TypedWorkflowLambda<T> workflow) {
        var definition = converter.toWorkflowDefinition(workflow);
        return persistAndQueue(definition);
    }

    @SneakyThrows
    private long persistAndQueue(WorkflowDefinition definition) {
        var task = new Execution();
        task.setStatus(ExecutionStatus.Queued);
        task.setQueuedAt(LocalDateTime.now());

        task.setDefinition(objectMapper.writeValueAsString(definition).getBytes());
        task.setParams(objectMapper.writeValueAsString(definition.getParameters()));

        return executions.save(task).getId();
    }
}
