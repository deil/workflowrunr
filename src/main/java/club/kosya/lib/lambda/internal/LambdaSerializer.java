package club.kosya.lib.lambda.internal;

import static org.springframework.util.ReflectionUtils.makeAccessible;

import club.kosya.lib.lambda.WorkflowLambda;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.List;

public class LambdaSerializer {
    public static SerializationResult serialize(WorkflowLambda lambda) {
        return serializeGeneric(lambda);
    }

    public static <T extends Serializable> SerializationResult serialize(T lambda) {
        return serializeGeneric(lambda);
    }

    private static <T> SerializationResult serializeGeneric(T lambda) {
        var serializedLambda = toSerializedLambda(lambda);
        var args = new ArrayList<Object>();
        for (var i = 0; i < serializedLambda.getCapturedArgCount(); i++) {
            args.add(serializedLambda.getCapturedArg(i));
        }

        return new SerializationResult(serializedLambdaToBytes(serializedLambda), args);
    }

    public static <T> SerializedLambda toSerializedLambda(T value) {
        if (!value.getClass().isSynthetic()) {
            throw new IllegalArgumentException(
                    "Please provide a lambda expression (e.g. BackgroundJob.enqueue(() -> myService.doWork()) instead of an actual implementation.");
        }

        try {
            var writeReplaceMethod = value.getClass().getDeclaredMethod("writeReplace");
            makeAccessible(writeReplaceMethod);
            return (SerializedLambda) writeReplaceMethod.invoke(value);
        } catch (Exception shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    public static byte[] serializedLambdaToBytes(SerializedLambda serializedLambda) {
        try (var baos = new ByteArrayOutputStream();
                var oos = new ObjectOutputStream(baos)) {
            var data = new SerializedLambdaData(
                    serializedLambda.getCapturingClass(),
                    serializedLambda.getFunctionalInterfaceClass(),
                    serializedLambda.getFunctionalInterfaceMethodName(),
                    serializedLambda.getFunctionalInterfaceMethodSignature(),
                    serializedLambda.getImplClass(),
                    serializedLambda.getImplMethodName(),
                    serializedLambda.getImplMethodSignature(),
                    serializedLambda.getImplMethodKind(),
                    serializedLambda.getInstantiatedMethodType(),
                    serializedLambda.getCapturedArgCount());

            oos.writeObject(data);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize lambda", e);
        }
    }

    record SerializedLambdaData(
            String capturingClass,
            String functionalInterfaceClass,
            String functionalInterfaceMethodName,
            String functionalInterfaceMethodSignature,
            String implClass,
            String implMethodName,
            String implMethodSignature,
            int implMethodKind,
            String instantiatedMethodType,
            int capturedArgsCount)
            implements Serializable {}

    public record SerializationResult(byte[] definition, List<Object> capturedArgs) {}
}
