package club.kosya.duraexec;

import club.kosya.lib.workflow.BeanReference;
import club.kosya.lib.workflow.WorkflowDefinition;
import club.kosya.lib.lambda.internal.LambdaMethodInvocationParser;
import club.kosya.lib.lambda.internal.TypedLambdaMethodInvocationParser;
import club.kosya.lib.lambda.TypedWorkflowLambda;
import club.kosya.lib.lambda.WorkflowLambda;
import club.kosya.lib.lambda.internal.ParameterSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import static club.kosya.lib.lambda.internal.LambdaSerializer.serialize;

/**
 * Converts workflow lambdas to WorkflowDefinition objects.
 * Handles both instance pattern (bean captured) and typed pattern (bean as parameter).
 * Pure conversion logic - no Spring dependencies needed.
 */
@Component
public class WorkflowDefinitionConverter {

    /**
     * Converts instance pattern lambda to WorkflowDefinition.
     * Pattern: workflow.run(() -> bean.method(ctx, params))
     * Extracts bean type from method invocation owner class.
     */
    public WorkflowDefinition toWorkflowDefinition(WorkflowLambda workflow) {
        var serializedData = serialize(workflow);
        var capturedArgs = serializedData.capturedArgs();

        var invocationInfo = LambdaMethodInvocationParser.parse(workflow, capturedArgs);

        var beanClassName = invocationInfo.getOwnerClass().replace('/', '.');

        var beanReference = new BeanReference(beanClassName);

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
        definition.setBeanReference(beanReference);
        definition.setMethodName(invocationInfo.getMethodName());
        definition.setMethodDescriptor(invocationInfo.getMethodDescriptor());
        definition.setParameters(methodParams);

        return definition;
    }

    /**
     * Converts typed pattern lambda to WorkflowDefinition.
     * Pattern: workflow.<MyService>run(x -> x.method(ctx, params))
     * Extracts bean type from method invocation owner class.
     */
    public <T> WorkflowDefinition toWorkflowDefinition(TypedWorkflowLambda<T> workflow) {
        var serializedData = serialize(workflow);
        var capturedArgs = serializedData.capturedArgs();

        var invocationInfo = TypedLambdaMethodInvocationParser.parse(workflow, capturedArgs);

        var beanClassName = invocationInfo.getOwnerClass().replace('/', '.');

        var beanReference = new BeanReference(beanClassName);

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
        definition.setBeanReference(beanReference);
        definition.setMethodName(invocationInfo.getMethodName());
        definition.setMethodDescriptor(invocationInfo.getMethodDescriptor());
        definition.setParameters(methodParams);

        return definition;
    }
}
