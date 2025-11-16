package club.kosya.lib.workflow.internal;

import club.kosya.lib.lambda.internal.LambdaMethodInvocationParser;
import club.kosya.lib.lambda.internal.TypedLambdaMethodInvocationParser;
import club.kosya.lib.lambda.TypedWorkflowLambda;
import club.kosya.lib.lambda.WorkflowLambda;
import club.kosya.lib.lambda.internal.ParameterSource;
import club.kosya.lib.workflow.ServiceIdentifier;
import club.kosya.lib.workflow.WorkflowDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import static club.kosya.lib.lambda.internal.LambdaSerializer.serialize;

@Component
public class WorkflowDefinitionConverter {

    public WorkflowDefinition toWorkflowDefinition(WorkflowLambda workflow) {
        var serializedData = serialize(workflow);
        var capturedArgs = serializedData.capturedArgs();

        var invocationInfo = LambdaMethodInvocationParser.parse(workflow, capturedArgs);

        var beanClassName = invocationInfo.getOwnerClass().replace('/', '.');

        var serviceIdentifier = new ServiceIdentifier(beanClassName);

        var methodParams = new ArrayList<>();
        var paramSources = invocationInfo.getParameterSources();

        for (ParameterSource source : paramSources) {
            if (source.isConstant()) {
                methodParams.add(source.getConstantValue());
            } else {
                methodParams.add(capturedArgs.get(source.getVariableIndex()));
            }
        }

        var definition = new WorkflowDefinition();
        definition.setServiceIdentifier(serviceIdentifier);
        definition.setMethodName(invocationInfo.getMethodName());
        definition.setMethodDescriptor(invocationInfo.getMethodDescriptor());
        definition.setParameters(methodParams);

        return definition;
    }

    public <T> WorkflowDefinition toWorkflowDefinition(TypedWorkflowLambda<T> workflow) {
        var serializedData = serialize(workflow);
        var capturedArgs = serializedData.capturedArgs();

        var invocationInfo = TypedLambdaMethodInvocationParser.parse(workflow, capturedArgs);

        var beanClassName = invocationInfo.getOwnerClass().replace('/', '.');

        var serviceIdentifier = new ServiceIdentifier(beanClassName);

        var methodParams = new ArrayList<>();
        var paramSources = invocationInfo.getParameterSources();

        for (ParameterSource source : paramSources) {
            if (source.isConstant()) {
                methodParams.add(source.getConstantValue());
            } else {
                methodParams.add(capturedArgs.get(source.getVariableIndex()));
            }
        }

        var definition = new WorkflowDefinition();
        definition.setServiceIdentifier(serviceIdentifier);
        definition.setMethodName(invocationInfo.getMethodName());
        definition.setMethodDescriptor(invocationInfo.getMethodDescriptor());
        definition.setParameters(methodParams);

        return definition;
    }
}