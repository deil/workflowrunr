package club.kosya.lib.workflow.internal;

import club.kosya.lib.workflow.ServiceInstanceProvider;
import club.kosya.lib.workflow.WorkflowDefinition;
import lombok.SneakyThrows;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class WorkflowReconstructor {
    private final ServiceInstanceProvider instanceProvider;

    public WorkflowReconstructor(ServiceInstanceProvider instanceProvider) {
        this.instanceProvider = instanceProvider;
    }

    public Object reconstructAndExecute(WorkflowDefinition definition, Object executionContext) {
        var bean = instanceProvider.getInstance(definition.getServiceIdentifier());

        var methodArgs = new ArrayList<>();
        methodArgs.add(executionContext);
        methodArgs.addAll(definition.getParameters());

        return invokeMethod(
                bean,
                definition.getMethodName(),
                definition.getMethodDescriptor(),
                methodArgs.toArray()
        );
    }

    @SneakyThrows
    private Object invokeMethod(Object bean, String methodName, String methodDescriptor, Object[] args) {
        var paramTypes = parseParameterTypes(methodDescriptor);

        var method = bean.getClass().getMethod(methodName, paramTypes);
        method.setAccessible(true);

        return method.invoke(bean, args);
    }

    private Class<?>[] parseParameterTypes(String descriptor) {
        var types = new ArrayList<Class<?>>();

        var i = 1;
        while (descriptor.charAt(i) != ')') {
            var c = descriptor.charAt(i);

            var arrayDimensions = 0;
            while (c == '[') {
                arrayDimensions++;
                c = descriptor.charAt(++i);
            }

            Class<?> baseType;

            if (c == 'L') {
                var semicolon = descriptor.indexOf(';', i);
                var className = descriptor.substring(i + 1, semicolon).replace('/', '.');
                try {
                    baseType = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Class not found: " + className, e);
                }
                i = semicolon + 1;
            } else {
                baseType = switch (c) {
                    case 'Z' -> boolean.class;
                    case 'B' -> byte.class;
                    case 'C' -> char.class;
                    case 'S' -> short.class;
                    case 'I' -> int.class;
                    case 'J' -> long.class;
                    case 'F' -> float.class;
                    case 'D' -> double.class;
                    case 'V' -> void.class;
                    default -> throw new IllegalArgumentException("Unknown type: " + c);
                };
                i++;
            }

            var finalType = baseType;
            for (var j = 0; j < arrayDimensions; j++) {
                finalType = Array.newInstance(finalType, 0).getClass();
            }

            types.add(finalType);
        }

        return types.toArray(new Class<?>[0]);
    }
}