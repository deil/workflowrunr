package club.kosya.lib.workflow;

import lombok.Data;

@Data
public class WorkflowParameter {
    private String name;
    private String type;
    private Object value;
}
