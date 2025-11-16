package club.kosya.lib.lambda.internal;

import java.util.List;

/**
 * Represents information about a method invocation extracted from lambda bytecode.
 */
public class MethodInvocationInfo {
    private final int targetVarIndex;
    private final String ownerClass;
    private final String methodName;
    private final String methodDescriptor;
    private final List<ParameterSource> parameterSources;

    public MethodInvocationInfo(int targetVarIndex, String ownerClass, String methodName, 
                           String methodDescriptor, List<ParameterSource> parameterSources) {
        this.targetVarIndex = targetVarIndex;
        this.ownerClass = ownerClass;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.parameterSources = parameterSources;
    }

    public int getTargetVarIndex() {
        return targetVarIndex;
    }

    public String getOwnerClass() {
        return ownerClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public java.util.List<ParameterSource> getParameterSources() {
        return parameterSources;
    }
}