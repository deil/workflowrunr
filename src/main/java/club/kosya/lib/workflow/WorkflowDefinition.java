package club.kosya.lib.workflow;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowDefinition {
    private BeanReference beanReference;
    private String methodName;
    private String methodDescriptor;
    private List<Object> parameters;
}
