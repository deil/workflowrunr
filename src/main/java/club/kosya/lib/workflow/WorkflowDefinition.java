package club.kosya.lib.workflow;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowDefinition {
    private ServiceIdentifier serviceIdentifier;
    private String methodName;
    private List<WorkflowParameter> parameters;
}
