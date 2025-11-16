package club.kosya.lib.lambda;

import java.io.Serializable;

@FunctionalInterface
public interface TypedWorkflowLambda<T> extends Serializable {
    void accept(T service);
}
