package club.kosya.duraexec.internal;

import lombok.Data;

@Data
public class ExecutionResult {
    private final int exitCode;
    private final String output;

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
