package club.kosya.lib.lambda.internal;

import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for parsing lambda bytecode using ASM to extract method invocation information.
 * Contains the shared parsing logic used by both lambda and typed lambda parsers.
 */
public abstract class BaseLambdaMethodInvocationParser {

    // Wrapper to distinguish constants from variable indices
    protected static class ConstantValue {
        final Object value;
        ConstantValue(Object value) {
            this.value = value;
        }
    }

    protected static MethodInvocationInfo parseLambdaBytecode(
            SerializedLambda serializedLambda,
            List<Object> capturedArgs) {
        return parseLambdaBytecode(serializedLambda, capturedArgs, false);
    }

    protected static MethodInvocationInfo parseLambdaBytecode(
            SerializedLambda serializedLambda,
            List<Object> capturedArgs,
            boolean isTypedLambda) {
        String capturingClassName = serializedLambda.getCapturingClass().replace('/', '.');
        String lambdaMethodName = serializedLambda.getImplMethodName();

        try {
            var capturingClass = Class.forName(capturingClassName);
            var classResourceName = "/" + serializedLambda.getCapturingClass() + ".class";

            try (var classInputStream = capturingClass.getResourceAsStream(classResourceName)) {
                if (classInputStream == null) {
                    throw new RuntimeException("Could not find class resource: " + classResourceName);
                }

                var classReader = new ClassReader(classInputStream);
                var extractor = new MethodInvocationExtractor(lambdaMethodName, capturedArgs, isTypedLambda);
                classReader.accept(extractor, ClassReader.SKIP_FRAMES);

                return extractor.getMethodInvocationInfo();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to parse lambda bytecode", e);
        }
    }

    /**
     * Extracts bean class name from functional interface method signature.
     * E.g., "(LMyService;)V" -> "MyService"
     */
    protected static String extractBeanClassName(String signature) {
        // Signature format: (LclassName;)V
        var start = signature.indexOf('L') + 1;
        var end = signature.indexOf(';');
        return signature.substring(start, end).replace('/', '.');
    }

    protected static class MethodInvocationExtractor extends ClassVisitor {
        private final String targetMethodName;
        private final List<Object> capturedArgs;
        private final boolean isTypedLambda;
        private MethodInvocationInfo methodInvocationInfo;

        public MethodInvocationExtractor(String targetMethodName, List<Object> capturedArgs, boolean isTypedLambda) {
            super(Opcodes.ASM9);
            this.targetMethodName = targetMethodName;
            this.capturedArgs = capturedArgs;
            this.isTypedLambda = isTypedLambda;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals(targetMethodName)) {
                return new MethodInvocationVisitor(capturedArgs, isTypedLambda);
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        public MethodInvocationInfo getMethodInvocationInfo() {
            if (methodInvocationInfo == null) {
                throw new RuntimeException("No method invocation found in lambda");
            }
            return methodInvocationInfo;
        }

        protected class MethodInvocationVisitor extends MethodVisitor {
            private final List<Object> stack = new ArrayList<>();
            private final List<Object> capturedArgs;
            private final boolean isTypedLambda;

            public MethodInvocationVisitor(List<Object> capturedArgs, boolean isTypedLambda) {
                super(Opcodes.ASM9);
                this.capturedArgs = capturedArgs;
                this.isTypedLambda = isTypedLambda;
            }

            /**
             * Gets captured argument by variable index.
             * For typed lambdas: variable 0 = bean parameter (not captured), variable 1+ = captured args
             * For instance lambdas: variable 0+ = captured args
             */
            private Object getCapturedArg(int varIndex) {
                int adjustedIndex = varIndex;
                if (isTypedLambda && varIndex > 0) {
                    adjustedIndex = varIndex - 1; // Skip bean parameter at index 0
                }
                
                if (adjustedIndex >= 0 && adjustedIndex < capturedArgs.size()) {
                    return capturedArgs.get(adjustedIndex);
                }
                throw new IndexOutOfBoundsException("Variable index " + varIndex + " (adjusted: " + adjustedIndex + 
                    ") out of bounds for captured args size " + capturedArgs.size());
            }

            @Override
            public void visitVarInsn(int opcode, int var) {
                // Track ALL load instructions (references and primitives)
                if (opcode == Opcodes.ALOAD ||  // reference
                    opcode == Opcodes.ILOAD ||  // int
                    opcode == Opcodes.LLOAD ||  // long
                    opcode == Opcodes.FLOAD ||  // float
                    opcode == Opcodes.DLOAD) {  // double
                    stack.add(var); // Add variable index
                }
            }

            @Override
            public void visitInsn(int opcode) {
                // Handle stack manipulation and constant instructions
                if (opcode == Opcodes.DUP) {
                    // Duplicate top stack value
                    if (!stack.isEmpty()) {
                        Object top = stack.get(stack.size() - 1);
                        stack.add(top);
                    }
                } else if (opcode == Opcodes.ACONST_NULL) {
                    // Push null constant
                    stack.add(new ConstantValue(null));
                } else if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
                    // Integer constants (-1 to 5)
                    int value = opcode - Opcodes.ICONST_0;
                    stack.add(new ConstantValue(value));
                } else if (opcode == Opcodes.LCONST_0 || opcode == Opcodes.LCONST_1) {
                    // Long constants (0 or 1)
                    long value = opcode - Opcodes.LCONST_0;
                    stack.add(new ConstantValue(value));
                } else if (opcode == Opcodes.FCONST_0 || opcode == Opcodes.FCONST_1 || opcode == Opcodes.FCONST_2) {
                    // Float constants (0.0, 1.0, or 2.0)
                    float value = opcode - Opcodes.FCONST_0;
                    stack.add(new ConstantValue(value));
                } else if (opcode == Opcodes.DCONST_0 || opcode == Opcodes.DCONST_1) {
                    // Double constants (0.0 or 1.0)
                    double value = opcode - Opcodes.DCONST_0;
                    stack.add(new ConstantValue(value));
                }
            }

            @Override
            public void visitLdcInsn(Object value) {
                // Track constant loads (string literals, numeric constants, etc.)
                stack.add(new ConstantValue(value));
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (opcode == Opcodes.GETSTATIC) {
                    // Track static field access (e.g., ExecutionContext.Placeholder)
                    stack.add(new ConstantValue(null));
                } else if (opcode == Opcodes.GETFIELD) {
                    // Track instance field access - pop target, push result
                    Object targetItem = stack.remove(stack.size() - 1);
                    Object target = null;
                    if (targetItem instanceof Integer) {
                        target = getCapturedArg((Integer) targetItem);
                    } else if (targetItem instanceof ConstantValue) {
                        target = ((ConstantValue) targetItem).value;
                    }

                    // Execute field access via reflection
                    Object result = getFieldValue(target, name);
                    stack.add(new ConstantValue(result));
                }
            }

            private Object getFieldValue(Object target, String fieldName) {
                try {
                    if (target == null) {
                        return null;
                    }
                    var field = target.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (opcode == Opcodes.INVOKESTATIC) {
                    // Static method call - no target object on stack
                    int paramCount = countParameters(descriptor);

                    if (stack.size() < paramCount) {
                        throw new RuntimeException("Not enough items on stack for static method invocation");
                    }

                    var startIndex = stack.size() - paramCount;

                    // Get parameters
                    var params = new ArrayList<>();
                    for (int i = 0; i < paramCount; i++) {
                        Object stackItem = stack.get(startIndex + i);
                        if (stackItem instanceof ConstantValue) {
                            params.add(((ConstantValue) stackItem).value);
                        } else {
                            params.add(getCapturedArg((Integer) stackItem));
                        }
                    }

                    // Remove consumed parameters from stack
                    for (int i = 0; i < paramCount; i++) {
                        stack.remove(stack.size() - 1);
                    }

                    // Skip Kotlin compiler intrinsics
                    if (isKotlinIntrinsic(owner, name)) {
                        // Do not push anything on stack for Kotlin checks
                        return;
                    }

                    // Execute static method via reflection
                    Object result = invokeStaticMethod(owner, name, descriptor, params.toArray());
                    stack.add(new ConstantValue(result));

                } else if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
                    int paramCount = countParameters(descriptor);
                    int totalArgs = paramCount + 1;

                    if (stack.size() < totalArgs) {
                        throw new RuntimeException("Not enough items on stack for method invocation");
                    }

                    int startIndex = stack.size() - totalArgs;
                    Object targetItem = stack.get(startIndex);

                    // Get target object
                    Object target = null;
                    int targetVarIndex = -1;
                    if (targetItem instanceof Integer) {
                        targetVarIndex = (Integer) targetItem;
                        target = getCapturedArg(targetVarIndex);
                    } else if (targetItem instanceof ConstantValue) {
                        target = ((ConstantValue) targetItem).value;
                    }

                    // Get parameters
                    var params = new ArrayList<>();
                    for (int i = 0; i < paramCount; i++) {
                        var stackItem = stack.get(startIndex + 1 + i);
                        if (stackItem instanceof ConstantValue) {
                            params.add(((ConstantValue) stackItem).value);
                        } else {
                            params.add(getCapturedArg((Integer) stackItem));
                        }
                    }

                    // Execute method via reflection
                    Object result = invokeMethod(target, name, descriptor, params.toArray());

                    // Track this invocation - last one wins
                    var paramSources = new ArrayList<ParameterSource>();
                    for (int i = 0; i < paramCount; i++) {
                        var stackItem = stack.get(startIndex + 1 + i);
                        if (stackItem instanceof ConstantValue) {
                            paramSources.add(ParameterSource.fromConstant(((ConstantValue) stackItem).value));
                        } else {
                            paramSources.add(ParameterSource.fromVariable((Integer) stackItem));
                        }
                    }

                    methodInvocationInfo = new MethodInvocationInfo(
                        targetVarIndex,
                        owner,
                        name,
                        descriptor,
                        paramSources
                    );

                    // Pop target + params, push result
                    for (int i = 0; i < totalArgs; i++) {
                        stack.remove(stack.size() - 1);
                    }
                    stack.add(new ConstantValue(result));
                }
            }

            private Object invokeMethod(Object target, String methodName, String descriptor, Object[] params) {
                try {
                    if (target == null) {
                        return null;
                    }
                    var clazz = target.getClass();
                    var paramTypes = parseParameterTypes(descriptor);
                    var method = clazz.getMethod(methodName, paramTypes);
                    method.setAccessible(true);
                    return method.invoke(target, params);
                } catch (Exception e) {
                    // Return null if execution fails (e.g., target method call that shouldn't execute)
                    return null;
                }
            }

            private Object invokeStaticMethod(String owner, String methodName, String descriptor, Object[] params) {
                try {
                    var className = owner.replace('/', '.');
                    var clazz = Class.forName(className);
                    var paramTypes = parseParameterTypes(descriptor);
                    var method = clazz.getMethod(methodName, paramTypes);
                    method.setAccessible(true);
                    Object result = method.invoke(null, params); // null target for static methods
                    return result;
                } catch (Exception e) {
                    return null;
                }
            }

            private boolean isKotlinIntrinsic(String owner, String methodName) {
                String className = owner.replace('/', '.');
                return className.startsWith("kotlin.") && (
                    methodName.startsWith("checkNotNull") ||
                    methodName.startsWith("throwUninitializedPropertyAccessException")
                );
            }

            private Class<?>[] parseParameterTypes(String descriptor) {
                var types = new ArrayList<>();

                var i = 1; // Skip opening '('
                while (descriptor.charAt(i) != ')') {
                    var c = descriptor.charAt(i);

                    // Handle array types
                    var arrayDimensions = 0;
                    while (c == '[') {
                        arrayDimensions++;
                        c = descriptor.charAt(++i);
                    }

                    Class<?> baseType;

                    // Handle object types
                    if (c == 'L') {
                        var semicolon = descriptor.indexOf(';', i);
                        var className = descriptor.substring(i + 1, semicolon).replace('/', '.');
                        try {
                            baseType = Class.forName(className);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Class not found: " + className, e);
                        }
                        i = semicolon + 1;
                    } else {
                        // Handle primitive types
                        baseType = switch (c) {
                            case 'Z' -> boolean.class;
                            case 'B' -> byte.class;
                            case 'C' -> char.class;
                            case 'S' -> short.class;
                            case 'I' -> int.class;
                            case 'J' -> long.class;
                            case 'F' -> float.class;
                            case 'D' -> double.class;
                            case 'V' -> void.class;
                            default -> throw new IllegalArgumentException("Unknown type: " + c);
                        };
                        i++;
                    }

                    var finalType = baseType;
                    for (int j = 0; j < arrayDimensions; j++) {
                        finalType = java.lang.reflect.Array.newInstance(finalType, 0).getClass();
                    }

                    types.add(finalType);
                }

                return types.toArray(new Class<?>[0]);
            }

            private int countParameters(String descriptor) {
                return parseParameterTypes(descriptor).length;
            }
        }
    }
}