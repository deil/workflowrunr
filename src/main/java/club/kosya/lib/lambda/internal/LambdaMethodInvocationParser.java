package club.kosya.lib.lambda.internal;

import club.kosya.lib.lambda.WorkflowLambda;

import java.util.List;

import static club.kosya.lib.lambda.internal.LambdaSerializer.toSerializedLambda;

/**
 * Parses lambda bytecode using ASM to extract method invocation information.
 * Identifies which bean method is being called and what parameters are passed.
 */
public class LambdaMethodInvocationParser extends BaseLambdaMethodInvocationParser {

    public static MethodInvocationInfo parse(WorkflowLambda lambda, List<Object> capturedArgs) {
        var serializedLambda = toSerializedLambda(lambda);
        return parseLambdaBytecode(serializedLambda, capturedArgs);
    }
}