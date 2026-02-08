package club.kosya.lib.lambda.internal;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
