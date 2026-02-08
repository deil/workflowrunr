package club.kosya.lib.workflow;

import java.util.List;
import lombok.Data;

@Data
public class WorkflowDefinition {
    private ServiceIdentifier serviceIdentifier;
    private String methodName;
    private List<WorkflowParameter> parameters;
}
