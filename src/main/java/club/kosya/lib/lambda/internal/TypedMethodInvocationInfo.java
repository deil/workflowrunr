package club.kosya.lib.lambda.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Information about a typed lambda method invocation.
 * Extends MethodInvocationInfo with bean class information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypedMethodInvocationInfo {
    private String beanClassName;
    private String ownerClass;
    private String methodName;
    private String methodDescriptor;
    private List<ParameterSource> parameterSources;
}
