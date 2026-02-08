package club.kosya.lib.lambda.internal;

import lombok.Data;

@Data
public class ParameterSource {
    private final boolean isConstant;
    private final int variableIndex;
    private final Object constantValue;

    public static ParameterSource fromVariable(int index) {
        return new ParameterSource(false, index, null);
    }

    public static ParameterSource fromConstant(Object value) {
        return new ParameterSource(true, -1, value);
    }
}
