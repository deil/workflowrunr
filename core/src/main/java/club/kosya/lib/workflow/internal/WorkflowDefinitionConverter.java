package club.kosya.lib.workflow.internal;

import static club.kosya.lib.lambda.internal.LambdaSerializer.serialize;

import club.kosya.lib.lambda.TypedWorkflowLambda;
import club.kosya.lib.lambda.WorkflowLambda;
import club.kosya.lib.lambda.internal.LambdaMethodInvocationParser;
import club.kosya.lib.lambda.internal.TypedLambdaMethodInvocationParser;
import club.kosya.lib.workflow.ServiceIdentifier;
import club.kosya.lib.workflow.WorkflowDefinition;
import club.kosya.lib.workflow.WorkflowParameter;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class WorkflowDefinitionConverter {

    public WorkflowDefinition toWorkflowDefinition(WorkflowLambda workflow) {
        var serializedData = serialize(workflow);
        var capturedArgs = serializedData.capturedArgs();

        var invocationInfo = LambdaMethodInvocationParser.parse(workflow, capturedArgs);

        var beanClassName = invocationInfo.getOwnerClass().replace('/', '.');

        var serviceIdentifier = new ServiceIdentifier(beanClassName);

        var methodParams = new ArrayList<WorkflowParameter>();
        var paramSources = invocationInfo.getParameterSources();

        var paramInfo = getParameterInfo(beanClassName, invocationInfo.getMethodName(), paramSources.size());

        for (int i = 0; i < paramSources.size(); i++) {
            var source = paramSources.get(i);
            var param = new WorkflowParameter();

            param.setName(paramInfo.paramNames[i]);
            param.setType(paramInfo.typeNames[i]);

            if (source.isConstant()) {
                var constantValue = source.getConstantValue();
                param.setValue(constantValue);
            } else {
                var value = capturedArgs.get(source.getVariableIndex());
                param.setValue(value);
            }

            methodParams.add(param);
        }

        var definition = new WorkflowDefinition();
        definition.setServiceIdentifier(serviceIdentifier);
        definition.setMethodName(invocationInfo.getMethodName());
        definition.setParameters(methodParams);

        return definition;
    }

    public <T> WorkflowDefinition toWorkflowDefinition(TypedWorkflowLambda<T> workflow) {
        var serializedData = serialize(workflow);
        var capturedArgs = serializedData.capturedArgs();

        var invocationInfo = TypedLambdaMethodInvocationParser.parse(workflow, capturedArgs);

        var beanClassName = invocationInfo.getOwnerClass().replace('/', '.');

        var serviceIdentifier = new ServiceIdentifier(beanClassName);

        var methodParams = new ArrayList<WorkflowParameter>();
        var paramSources = invocationInfo.getParameterSources();

        var paramInfo = getParameterInfo(beanClassName, invocationInfo.getMethodName(), paramSources.size());

        for (int i = 0; i < paramSources.size(); i++) {
            var source = paramSources.get(i);
            var param = new WorkflowParameter();

            param.setName(paramInfo.paramNames[i]);
            param.setType(paramInfo.typeNames[i]);

            if (source.isConstant()) {
                var constantValue = source.getConstantValue();
                param.setValue(constantValue);
            } else {
                var value = capturedArgs.get(source.getVariableIndex());
                param.setValue(value);
            }

            methodParams.add(param);
        }

        var definition = new WorkflowDefinition();
        definition.setServiceIdentifier(serviceIdentifier);
        definition.setMethodName(invocationInfo.getMethodName());
        definition.setParameters(methodParams);

        return definition;
    }

    private ParameterInfo getParameterInfo(String className, String methodName, int paramCount) {
        try {
            var clazz = Class.forName(className);
            var methods = clazz.getMethods();

            for (var method : methods) {
                if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                    var paramTypes = method.getParameterTypes();
                    var paramNames = new String[paramTypes.length];
                    var typeNames = new String[paramTypes.length];

                    var parameters = method.getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        paramNames[i] = parameters[i].getName();
                        typeNames[i] = paramTypes[i].getName();
                    }

                    return new ParameterInfo(paramNames, typeNames);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }

        throw new IllegalArgumentException(
                "Method not found: " + methodName + " with " + paramCount + " parameters in " + className);
    }

    private record ParameterInfo(String[] paramNames, String[] typeNames) {}
}
