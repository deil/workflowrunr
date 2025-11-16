package club.kosya.lib.executionengine.internal;

import club.kosya.lib.executionengine.ExecutedAction;
import club.kosya.lib.executionengine.ExecutionFlow;
import club.kosya.lib.executionengine.ExecutionsRepository;
import club.kosya.lib.workflow.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
public class ExecutionContextImplementation implements ExecutionContext {
    private final ObjectMapper objectMapper;
    private final ExecutionsRepository executions;
    private final ExecutionFlow flow;
    private final Deque<Integer> actionCounterStack;

    public ExecutionContextImplementation(String id, ObjectMapper objectMapper, ExecutionsRepository executions) {
        this.objectMapper = objectMapper;
        this.executions = executions;
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
        action("sleep", () -> {
            try {
                Thread.sleep(duration);
                return null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public <R> R await(String name, Supplier<R> lambda) {
        return action(name, lambda);
    }

    @Override
    public <R> R action(String name, Supplier<R> lambda) {
        var actionId = generateActionId(name);
        var tracking = findOrCreateAction(actionId);
        tracking.setName(name);

        var action = new club.kosya.lib.executionengine.WorkflowAction(this, tracking.getId(), name);
        var result = action.execute(lambda::get);

        try {
            tracking.setResult(objectMapper.writeValueAsString(result));
            tracking.setResultType(result != null ? result.getClass().getName() : null);
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