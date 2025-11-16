package club.kosya.duraexec;

import club.kosya.duraexec.internal.ExecutedAction;
import club.kosya.duraexec.internal.ExecutionFlow;
import club.kosya.duraexec.internal.ExecutionsRepository;
import club.kosya.duraexec.internal.WorkflowAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class ExecutionContext {
    public final static ExecutionContext Placeholder = new ExecutionContext(null, null, null);

    private final ObjectMapper objectMapper;
    private final ExecutionsRepository executions;
    private final ExecutionFlow flow;
    private final Deque<Integer> actionCounterStack;

    public ExecutionContext(String id, ObjectMapper objectMapper, ExecutionsRepository executions) {
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

    @SneakyThrows
    private ExecutionFlow restoreOrCreateFlow(String id) {
        var execution = executions.findById(Long.parseLong(id));
        if (execution.isEmpty() || execution.get().getState() == null) {
            return new ExecutionFlow(id);
        }

        return objectMapper.readValue(execution.get().getState(), ExecutionFlow.class);
    }

    @SneakyThrows
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

    @SneakyThrows
    public <R> R await(String name, Supplier<R> lambda) {
        return action(name, lambda);
    }

    @SneakyThrows
    public <R> R action(String name, Supplier<R> lambda) {
        var actionId = generateActionId(name);
        var tracking = findOrCreateAction(actionId);
        tracking.setName(name);

        var action = new WorkflowAction(this, tracking.getId(), name);
        var result = action.execute(lambda::get);

        tracking.setResult(objectMapper.writeValueAsString(result));
        tracking.setResultType(result != null ? result.getClass().getName() : null);
        persistFlowState();

        return result;
    }

    @SneakyThrows
    private void persistFlowState() {
        var execution = executions.findById(Long.parseLong(flow.getId())).get();
        execution.setState(objectMapper.writeValueAsString(flow));
        executions.save(execution);
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

    String generateActionId(String name) {
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
                // Parent counters already incremented (point to next sibling), use counter - 1
                id.append(counter - 1);
            }
        }

        return id.toString();
    }

    private void incrementCurrentLevelCounter() {
        int current = actionCounterStack.pop();
        actionCounterStack.push(current + 1);
    }

    void enterAction(String actionId) {
        if (flow == null) return;

        actionCounterStack.push(0);
    }

    void exitAction() {
        if (flow == null) return;

        actionCounterStack.pop();
    }
}
