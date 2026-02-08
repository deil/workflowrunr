package club.kosya.lib.lambda;

import java.io.Serializable;

@FunctionalInterface
public interface WorkflowLambda extends Serializable {
    void run();
}
