package club.kosya.lib.executionengine.internal;

import club.kosya.lib.deserialization.ObjectDeserializer;
import club.kosya.lib.workflow.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.function.Supplier;

@Slf4j
public class ExecutionContextImpl implements ExecutionContext {
    private final ObjectMapper objectMapper;
    private final ExecutionsRepository executions;
    private final ExecutionFlow flow;
    private final Deque<Integer> actionCounterStack;
    private final ObjectDeserializer deserializerRegistry;

    public ExecutionContextImpl(String id, ObjectMapper objectMapper, ExecutionsRepository executions, ObjectDeserializer deserializerRegistry) {
        this.objectMapper = objectMapper;
        this.executions = executions;
        this.deserializerRegistry = deserializerRegistry;
        this.actionCounterStack = new ArrayDeque<>();

        if (id == null) {
            flow = null;
        } else {
            flow = restoreOrCreateFlow(id);
            actionCounterStack.push(0);
        }
    }

    private ExecutionFlow restoreOrCreateFlow(String id) {
        try {
            var execution = executions.findById(Long.parseLong(id));
            if (execution.isEmpty() || execution.get().getState() == null) {
                return new ExecutionFlow(id);
            }

            return objectMapper.readValue(execution.get().getState(), ExecutionFlow.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore flow", e);
        }
    }

    @Override
    public void sleep(Duration duration) {
        var actionId = generateActionId("sleep");
        var tracking = findOrCreateAction(actionId);
        tracking.setName("sleep");

        if (tracking.getCompleted()) {
            // Already slept, just return
            return;
        }

        // If wakeAt exists and is in the past, mark complete and return
        if (tracking.getWakeAt() != null && tracking.getWakeAt().isBefore(Instant.now())) {
            tracking.setCompleted(true);
            tracking.setResult("null");
            tracking.setWakeAt(null);  // Clear wakeAt after sleep completes
            
            // Clear execution wakeAt
            var execution = executions.findById(Long.parseLong(flow.getId())).get();
            execution.setWakeAt(null);
            executions.save(execution);
            
            persistFlowState();
            return;
        }

        // Calculate and store wake time
        var wakeAt = Instant.now().plus(duration);
        tracking.setWakeAt(wakeAt);

        // Store wakeAt on execution entity for scheduler
        var execution = executions.findById(Long.parseLong(flow.getId())).get();
        execution.setWakeAt(wakeAt);
        executions.save(execution);

        // Persist flow state with incomplete sleep action
        persistFlowState();

        // Return immediately - don't block thread
    }

    @Override
    public <R> R await(String name, Supplier<R> lambda) {
        return action(name, lambda);
    }

    public <R> R action(String name, Supplier<R> lambda) {
        var actionId = generateActionId(name);
        var tracking = findOrCreateAction(actionId);
        tracking.setName(name);

        if (tracking.getCompleted()) {
            try {
                return (R) deserializerRegistry.deserialize(tracking.getResultType(), tracking.getResult());
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize cached result", e);
            }
        }

        var action = new WorkflowAction(this, tracking.getId(), name);
        var result = action.execute(lambda::get);

        try {
            tracking.setResult(objectMapper.writeValueAsString(result));
            tracking.setResultType(result != null ? result.getClass().getName() : null);
            tracking.setCompleted(true);
            persistFlowState();
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist action result", e);
        }

        return result;
    }

    private void persistFlowState() {
        try {
            var execution = executions.findById(Long.parseLong(flow.getId())).get();
            execution.setState(objectMapper.writeValueAsString(flow));
            executions.save(execution);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist flow state", e);
        }
    }

    private ExecutedAction findOrCreateAction(String actionId) {
        return flow.getActions().stream()
                .filter(a -> a.getId().equals(actionId))
                .findFirst()
                .orElseGet(() -> {
                    var newAction = new ExecutedAction(actionId);
                    flow.getActions().add(newAction);
                    return newAction;
                });
    }

    public String generateActionId(String name) {
        if (flow == null) {
            return "placeholder";
        }

        String id = buildActionIdFromPath();
        incrementCurrentLevelCounter();

        return id;
    }

    private String buildActionIdFromPath() {
        var pathFromRoot = new ArrayList<>(actionCounterStack);
        Collections.reverse(pathFromRoot);

        var id = new StringBuilder();
        for (int level = 0; level < pathFromRoot.size(); level++) {
            if (level > 0) {
                id.append(".");
            }

            var counter = pathFromRoot.get(level);
            var isCurrentLevel = level == pathFromRoot.size() - 1;

            if (isCurrentLevel) {
                id.append(counter);
            } else {
                id.append(counter - 1);
            }
        }

        return id.toString();
    }

    private void incrementCurrentLevelCounter() {
        int current = actionCounterStack.pop();
        actionCounterStack.push(current + 1);
    }

    public void enterAction(String actionId) {
        if (flow == null) return;

        actionCounterStack.push(0);
    }

    public void exitAction() {
        if (flow == null) return;

        actionCounterStack.pop();
    }
}