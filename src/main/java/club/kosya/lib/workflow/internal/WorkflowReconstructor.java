package club.kosya.lib.workflow.internal;

import club.kosya.lib.deserialization.ObjectDeserializer;
import club.kosya.lib.workflow.ExecutionContext;
import club.kosya.lib.workflow.ServiceInstanceProvider;
import club.kosya.lib.workflow.WorkflowDefinition;
import java.util.ArrayList;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class WorkflowReconstructor {
    private final ServiceInstanceProvider instanceProvider;
    private final ObjectDeserializer objectDeserializer;

    public Object reconstructAndExecute(WorkflowDefinition definition, Supplier<ExecutionContext> executionCtxHolder) {
        var bean = instanceProvider.getInstance(definition.getServiceIdentifier());

        var methodArgs = new ArrayList<>();

        for (var param : definition.getParameters()) {
            if (param.getType() != null && param.getType().equals(ExecutionContext.class.getName())) {
                methodArgs.add(executionCtxHolder.get());
            } else {
                methodArgs.add(objectDeserializer.deserialize(
                        param.getType(),
                        param.getValue() != null ? param.getValue().toString() : null));
            }
        }

        return invokeMethod(bean, definition.getMethodName(), methodArgs.toArray());
    }

    @SneakyThrows
    private Object invokeMethod(Object bean, String methodName, Object[] args) {
        var paramTypes = getParameterTypes(bean, methodName, args.length - 1);

        var method = bean.getClass().getMethod(methodName, paramTypes);
        method.setAccessible(true);

        return method.invoke(bean, args);
    }

    private Class<?>[] getParameterTypes(Object bean, String methodName, int paramCount) {
        var methods = bean.getClass().getMethods();

        for (var method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == paramCount + 1) {
                return method.getParameterTypes();
            }
        }

        throw new IllegalArgumentException("Method not found: " + methodName + " with " + paramCount + " parameters");
    }
}
