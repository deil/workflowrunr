package club.kosya.lib.lambda.internal;

import club.kosya.lib.lambda.TypedWorkflowLambda;

import java.util.List;

import static club.kosya.lib.lambda.internal.LambdaSerializer.toSerializedLambda;

/**
 * Parses typed lambda bytecode using ASM to extract method invocation information.
 * For pattern: workflow.&lt;Bean&gt;run(x -&gt; x.method(ctx, params))
 */
public class TypedLambdaMethodInvocationParser extends BaseLambdaMethodInvocationParser {

    public static TypedMethodInvocationInfo parse(TypedWorkflowLambda<?> lambda, List<Object> capturedArgs) {
        var serializedLambda = toSerializedLambda(lambda);

        // Extract bean class from functional interface method signature
        var beanClassName = extractBeanClassName(serializedLambda.getFunctionalInterfaceMethodSignature());

        // Parse the lambda bytecode to get the base method invocation info
        var baseInfo = parseLambdaBytecode(serializedLambda, capturedArgs, true);

        return new TypedMethodInvocationInfo(
            beanClassName,
            baseInfo.getOwnerClass(),
            baseInfo.getMethodName(),
            baseInfo.getMethodDescriptor(),
            baseInfo.getParameterSources()
        );
    }
}