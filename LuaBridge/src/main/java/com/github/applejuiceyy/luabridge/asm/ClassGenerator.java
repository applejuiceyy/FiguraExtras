package com.github.applejuiceyy.luabridge.asm;

import com.github.applejuiceyy.luabridge.*;
import com.github.applejuiceyy.luabridge.annotation.IsIndex;
import com.github.applejuiceyy.luabridge.annotation.IsNullable;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public class ClassGenerator {
    private final ClassWriter classWriter;
    private final GeneratedWrap compiled;
    private final ASMDispatchGenerator generator;
    byte[] compiledClass;

    ClassGenerator(ASMDispatchGenerator generator, ASMDispatchGenerator.Node rootNode, int handleCount) {
        this.generator = generator;
        // dummy anonymous class because of classloaders
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
        };
        compiled = compile(rootNode, handleCount);
    }

    // LuaValue -> String
    private static void luaValueToDebugName(MethodVisitor visitor) {
        // LuaValue -> LuaValue LuaValue
        visitor.visitInsn(DUP);
        // LuaValue LuaValue -> LuaValue Z
        visitor.visitMethodInsn(
                INVOKEVIRTUAL,
                Consts.LUA_VALUE.getInternalName(),
                "isuserdata",
                "()Z",
                false
        );

        Label normalDataLabel = new Label();
        Label terminate = new Label();
        // LuaValue Z -> LuaValue
        visitor.visitJumpInsn(IFEQ, normalDataLabel);

        // LuaValue -> Object
        visitor.visitMethodInsn(INVOKEVIRTUAL,
                Consts.LUA_VALUE.getInternalName(),
                "checkuserdata",
                "()Ljava/lang/Object;",
                false
        );
        // Object -> class
        visitor.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/Object",
                "getClass",
                "()Ljava/lang/Class;",
                false
        );
        // class -> String
        visitor.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/Class",
                "getSimpleName",
                "()Ljava/lang/String;",
                false
        );
        visitor.visitJumpInsn(GOTO, terminate);
        visitor.visitLabel(normalDataLabel);
        // LuaValue -> String
        visitor.visitMethodInsn(INVOKEVIRTUAL,
                Consts.LUA_VALUE.getInternalName(),
                "typename",
                "()Ljava/lang/String;",
                false
        );
        visitor.visitLabel(terminate);
    }

    private static void pushLuaValue(MethodVisitor visitor, int varargsPos) {
        // Varargs -> LuaValue
        if (varargsPos == 1) {
            // Varargs -> LuaValue
            visitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "org/luaj/vm2/Varargs",
                    "arg1",
                    "()Lorg/luaj/vm2/LuaValue;",
                    false
            );
        } else {
            // Varargs -> Varargs I
            visitor.visitLdcInsn(varargsPos);
            // Varargs I -> LuaValue
            visitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "org/luaj/vm2/Varargs",
                    "arg",
                    "(I)Lorg/luaj/vm2/LuaValue;",
                    false
            );
        }
    }

    // LuaValue -> specified
    private void extractPrimitiveFromArgument(MethodVisitor visitor, String luaCheckMethod, Label continuer, String luaExtractMethod, String luaExtractMethodReturn) {
        // LuaValue -> LuaValue LuaValue
        visitor.visitInsn(DUP);
        // LuaValue LuaValue -> LuaValue Z
        visitor.visitMethodInsn(
                INVOKEVIRTUAL,
                Consts.LUA_VALUE.getInternalName(),
                luaCheckMethod,
                "()Z",
                false
        );
        // LuaValue Z -> LuaValue
        visitor.visitJumpInsn(IFEQ, continuer);
        // LuaValue -> specified
        visitor.visitMethodInsn(
                INVOKEVIRTUAL,
                Consts.LUA_VALUE.getInternalName(),
                luaExtractMethod,
                "()" + luaExtractMethodReturn,
                false
        );
    }

    // specified -> specified
    private void subtractPrimitiveIfIndex(MethodVisitor visitor, boolean isIndex, Object subtracting, int subtractionOpcode) {
        if (isIndex) {
            // specified -> specified -1 (specified)
            visitor.visitLdcInsn(subtracting);
            // specified -1 (specified) -> specified
            visitor.visitInsn(subtractionOpcode);
        }
    }

    // anything -> Varargs
    private void compileReturnValue(MethodVisitor visitor, Class<?> returnType, Annotation[] returnAnnotations) {
        Label after = new Label();
        boolean isIndex = Arrays.stream(returnAnnotations).anyMatch(p -> p instanceof IsIndex);

        if (returnType == void.class) {
            visitor.visitFieldInsn(GETSTATIC, Consts.LUA_VALUE.getInternalName(), "NIL", Consts.LUA_VALUE.getDescriptor());
            return;
        }

        if (!returnType.isPrimitive()) {
            // Object -> Object Object
            visitor.visitInsn(DUP);
            Label pass = new Label();
            // Object Object -> Object
            visitor.visitJumpInsn(IFNONNULL, pass);
            // null ->
            visitor.visitInsn(POP);
            if (Arrays.stream(returnAnnotations).anyMatch(p -> p instanceof IsNullable)) {
                // -> nil
                visitor.visitFieldInsn(GETSTATIC, Consts.LUA_VALUE.getInternalName(), "NIL", Consts.LUA_VALUE.getDescriptor());
                // nil -> nil
                visitor.visitJumpInsn(GOTO, after);
            } else {
                // -> Class
                visitor.visitTypeInsn(NEW, "java/lang/IllegalStateException");
                // Class -> Class Class
                visitor.visitInsn(DUP);
                // Class Class -> Class Class String
                visitor.visitLdcInsn("Non-nullable method returned null");
                // Class Class String -> IAE
                visitor.visitMethodInsn(
                        INVOKESPECIAL,
                        "java/lang/IllegalStateException",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false
                );

                // IAE ->
                visitor.visitInsn(ATHROW);
            }

            visitor.visitLabel(pass);
        }
        // Object
        if (returnType == Object.class) {
            // Object -> Object int
            visitor.visitLdcInsn(isIndex);
            // Object int -> int Object
            visitor.visitInsn(SWAP);
            // int Object -> int Object Bridge
            visitor.visitVarInsn(ALOAD, 2);
            // int Object Bridge -> Bridge int Object Bridge
            visitor.visitInsn(DUP_X2);
            // Bridge int Object Bridge -> Bridge int Object
            visitor.visitInsn(POP);
            // Bridge int Object -> Bridge int Object 1
            visitor.visitInsn(ICONST_1);
            // Bridge int Object 1 -> Bridge int Object Object[]
            visitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(Object.class));
            // Bridge int Object Object[] -> Bridge int Object[] Object Object[]
            visitor.visitInsn(DUP_X1);
            // Bridge int Object[] Object Object[] -> Bridge int Object[] Object Object[] 0
            visitor.visitInsn(ICONST_0);
            // Bridge int Object[] Object Object[] 0 -> Bridge int Object[] Object[] 0 Object Object[] 0
            visitor.visitInsn(DUP2_X1);
            // Bridge int Object[] Object[] 0 Object Object[] 0 -> Bridge int Object[] Object[] 0 Object
            visitor.visitInsn(POP2);
            // Bridge int Object[] Object[] 0 Object -> Bridge int Object[]
            visitor.visitInsn(AASTORE);
            // Bridge int Object[] -> Varargs
            visitor.visitMethodInsn(
                    INVOKEINTERFACE,
                    Type.getType(Converter.class).getInternalName(),
                    "toLuaVarargs",
                    "(Z[Ljava/lang/Object;)" + Consts.VARARGS.getDescriptor(),
                    true
            );
            visitor.visitLabel(after);
            return;
        }
        String name = returnType.getName();
        switch (name) {
            case "java.lang.Double", "double" -> {
                // D/Double
                if (name.equals("java.lang.Double")) {
                    // Double -> D
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Double",
                            "doubleValue",
                            "()D",
                            false
                    );
                }
                // D -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(D)Lorg/luaj/vm2/LuaNumber;",
                        false
                );
            }
            case "java.lang.String" -> {
                // String -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(Ljava/lang/String;)Lorg/luaj/vm2/LuaString;",
                        false
                );
            }
            case "java.lang.Boolean", "boolean" -> {
                // I/Boolean
                if (name.equals("java.lang.Boolean")) {
                    // Boolean -> I
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Boolean",
                            "booleanValue",
                            "()Z",
                            false
                    );
                }
                // I -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(Z)Lorg/luaj/vm2/LuaBoolean;",
                        false
                );
            }
            case "java.lang.Float", "float" -> {
                // F/Float
                if (name.equals("java.lang.Float")) {
                    // Float -> F
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Float",
                            "floatValue",
                            "()F",
                            false
                    );
                }
                // F -> D
                visitor.visitInsn(F2D);
                // D -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(D)Lorg/luaj/vm2/LuaNumber;",
                        false
                );
            }
            case "java.lang.Integer", "int" -> {
                // I/Integer
                if (name.equals("java.lang.Integer")) {
                    // Integer -> I
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Integer",
                            "intValue",
                            "()I",
                            false
                    );
                }
                if (isIndex) {
                    // I -> I I
                    visitor.visitLdcInsn(1);
                    // I I -> I
                    visitor.visitInsn(IADD);
                }
                // I -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(I)Lorg/luaj/vm2/LuaInteger;",
                        false
                );
            }
            case "java.lang.Long", "long" -> {
                // J/Long
                if (name.equals("java.lang.Long")) {
                    // Long -> J
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Long",
                            "longValue",
                            "()J",
                            false
                    );
                }
                // J -> I
                visitor.visitInsn(L2I);
                if (isIndex) {
                    // I -> I I
                    visitor.visitLdcInsn(1);
                    // I I -> I
                    visitor.visitInsn(IADD);
                }
                // I -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(I)Lorg/luaj/vm2/LuaInteger;",
                        false
                );
            }
            case "java.lang.Byte", "byte" -> {
                // I/Byte
                if (name.equals("java.lang.Byte")) {
                    // Byte -> I
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Byte",
                            "byteValue",
                            "()B",
                            false
                    );
                }
                if (isIndex) {
                    // I -> I I
                    visitor.visitLdcInsn(1);
                    // I I -> I
                    visitor.visitInsn(IADD);
                }
                // I -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(I)Lorg/luaj/vm2/LuaInteger;",
                        false
                );
            }
            case "java.lang.Short", "short" -> {
                // I/Short
                if (name.equals("java.lang.Short")) {
                    // Short -> I
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Short",
                            "shortValue",
                            "()S",
                            false
                    );
                }
                if (isIndex) {
                    // I -> I I
                    visitor.visitLdcInsn(1);
                    // I I -> I
                    visitor.visitInsn(IADD);
                }
                // I -> LuaValue
                visitor.visitMethodInsn(
                        INVOKESTATIC,
                        Consts.LUA_VALUE.getInternalName(),
                        "valueOf",
                        "(I)Lorg/luaj/vm2/LuaInteger;",
                        false
                );
            }
            default -> {
                if (name.startsWith("org/luaj/vm2/")) {
                    String replace = name.replace('/', '.');
                    boolean assignableFrom = false;
                    try {
                        Class<?> cls = Class.forName(replace);
                        assignableFrom = Varargs.class.isAssignableFrom(cls);
                    } catch (ClassNotFoundException ignored) {
                    }
                    if (assignableFrom) {
                        // Varargs
                        // nothing to do
                        return;
                    }
                }
                // Object -> Object Bridge
                visitor.visitVarInsn(ALOAD, 2);
                // Object Bridge -> Bridge Object Bridge
                visitor.visitInsn(DUP_X1);
                // Bridge Object Bridge -> Bridge Object
                visitor.visitInsn(POP);
                // Bridge Object -> LuaValue
                visitor.visitMethodInsn(
                        INVOKEINTERFACE,
                        Consts.HOOK_NAME.getInternalName(),
                        "wrap",
                        "(Ljava/lang/Object;)Lorg/luaj/vm2/LuaValue;",
                        true
                );
            }
        }
        visitor.visitLabel(after);
    }

    // Varargs -> Varargs
    private void emitExitHook(MethodVisitor visitor) {
        // Varargs -> Varargs Varargs
        visitor.visitInsn(DUP);
        // Varargs Varargs -> Varargs Varargs Bridge
        visitor.visitVarInsn(ALOAD, 2);
        // Varargs Varargs Bridge -> Varargs Bridge Varargs Bridge
        visitor.visitInsn(DUP_X1);
        // Varargs Bridge Varargs Bridge -> Varargs Bridge Varargs
        visitor.visitInsn(POP);
        // Varargs Bridge Varargs -> Varargs Bridge Varargs Varargs
        visitor.visitInsn(DUP);
        // Varargs Bridge Varargs Varargs -> Varargs Bridge Varargs
        visitor.visitVarInsn(ASTORE, 2);
        // Varargs Bridge Varargs -> Varargs
        ASMUtil.emitTryCatch(visitor, () -> visitor.visitMethodInsn(
                INVOKEINTERFACE,
                Consts.HOOK_NAME.getInternalName(),
                "exit",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Varargs.class)),
                true
        )).emitCatch(Type.getType(Throwable.class), () -> {
            emitIgnoreError(visitor, "Ignoring error during exit hook");
            visitor.visitVarInsn(ALOAD, 2);
        }).emit();
    }

    // Throwable -> Throwable
    private void emitErrorHook(MethodVisitor visitor) {
        // Throwable -> Throwable Throwable
        visitor.visitInsn(DUP);
        // Throwable Throwable -> Throwable Throwable Bridge
        visitor.visitVarInsn(ALOAD, 2);
        // Throwable Throwable Bridge -> Throwable Bridge Throwable Bridge
        visitor.visitInsn(DUP_X1);
        // Throwable Bridge Throwable Bridge -> Throwable Bridge Throwable
        visitor.visitInsn(POP);
        // Throwable Bridge Throwable -> Throwable Bridge Throwable Throwable
        visitor.visitInsn(DUP);
        // Throwable Bridge Throwable Throwable -> Throwable Bridge Throwable
        visitor.visitVarInsn(ASTORE, 2);
        // Throwable Bridge Throwable -> Throwable
        ASMUtil.emitTryCatch(visitor, () -> visitor.visitMethodInsn(
                                INVOKEINTERFACE,
                                Consts.HOOK_NAME.getInternalName(),
                                "error",
                                "(Ljava/lang/Throwable;)V",
                                true
                        )
                )
                .emitCatch(Type.getType(Throwable.class), () -> {
                    emitIgnoreError(visitor, "Ignoring error during error hook");
                    visitor.visitVarInsn(ALOAD, 2);
                })
                .emit();
    }

    // specified -> Box
    public void upgradeToBoxIfAvailable(MethodVisitor visitor, Class<?> box, Class<?> primitive, String current) {
        Type t = Type.getType(box);

        if (t.getClassName().equals(current)) {
            // specified -> Box
            visitor.visitMethodInsn(
                    INVOKESTATIC,
                    t.getInternalName(),
                    "valueOf",
                    Type.getMethodDescriptor(t, Type.getType(primitive)),
                    false
            );
        }
    }

    // ->
    private void overloadFollowFail(MethodVisitor visitor, int varargsPos, ASMDispatchGenerator.Node node, Runnable runnable, Runnable otherwise) {
        visitor.visitLdcInsn(varargsPos);
        overloadFollowFail(visitor, node, runnable, otherwise);
    }

    private Class<?> unbox(Class<?> cls) {
        int i = TypeInfo.find(TypeInfo.PRIMITIVE_BOXES, cls);
        if (i != -1) {
            return TypeInfo.PRIMITIVES[i];
        }
        return cls;
    }

    // I ->
    private void overloadFollowFail(MethodVisitor visitor, ASMDispatchGenerator.Node node, Runnable runnable, Runnable otherwise) {
        Label unlikelyOverload = new Label();
        // I -> I I
        visitor.visitVarInsn(ILOAD, 4);
        // I I -> I I
        visitor.visitInsn(SWAP);
        // I I -> I I I
        visitor.visitInsn(DUP_X1);
        // I I I -> I
        visitor.visitJumpInsn(IF_ICMPGE, unlikelyOverload);
        // I ->
        visitor.visitVarInsn(ISTORE, 4);

        // -> String

        String[] strings = Streams.concat(
                node.types.keySet().stream().map(this::unbox).map(Class::getSimpleName),
                node.executorsByVarargs.keySet().stream().filter(Objects::nonNull).map(Pair::getLeft).map(this::unbox).map(p -> "varargs of " + p.getSimpleName()),
                node.ifNull == null ? Stream.empty() : Stream.of("nil"),
                node.executorsByVarargs.containsKey(null) ? Stream.of("nothing") : Stream.empty()
        ).toArray(String[]::new);
        String string;
        if (strings.length == 1) {
            string = strings[0];
        } else {
            string = Arrays.stream(strings, 0, strings.length - 1).collect(Collectors.joining(", ")) + " or " + strings[strings.length - 1];
        }
        // -> String
        visitor.visitLdcInsn(string);
        // String ->
        visitor.visitVarInsn(ASTORE, 7);
        // ->
        runnable.run();
        Label fin = new Label();
        visitor.visitJumpInsn(GOTO, fin);
        visitor.visitLabel(unlikelyOverload);
        // I ->
        visitor.visitInsn(POP);
        // ->
        otherwise.run();
        visitor.visitLabel(fin);
    }

    private void compileLuaToJavaDecoding(MethodVisitor visitor, Runnable luaValuePusher, Map<Pair<Class<?>, Annotation[]>, BooleanSupplier> types) {
        ArrayList<Map.Entry<Pair<Class<?>, Annotation[]>, BooleanSupplier>> leftover = new ArrayList<>();
        for (Map.Entry<Pair<Class<?>, Annotation[]>, BooleanSupplier> entry : types.entrySet()) {
            Pair<Class<?>, Annotation[]> key = entry.getKey();
            Class<?> type = key.getLeft();
            Annotation[] annotations = key.getRight();
            BooleanSupplier runnable = entry.getValue();

            CTMethodArgumentConverterHandler handler = generator.argumentConverters.findFirst(type);
            if (handler != null) {
                Label continuer = new Label();
                ArgumentConverterContext context = new ArgumentConverterContext(continuer, visitor, classWriter, generator);

                handler.convert(type, annotations, context);

                if (!runnable.getAsBoolean()) {
                    // anything ->
                    visitor.visitInsn(Type.getType(type).getSize() == 2 ? POP2 : POP);
                    // -> LuaValue
                    luaValuePusher.run();
                }
                ;

                visitor.visitLabel(continuer);
            } else {
                leftover.add(entry);
            }
        }
        for (Iterator<Map.Entry<Pair<Class<?>, Annotation[]>, BooleanSupplier>> iterator = leftover.iterator(); iterator.hasNext(); ) {
            Map.Entry<Pair<Class<?>, Annotation[]>, BooleanSupplier> entry = iterator.next();
            Pair<Class<?>, Annotation[]> key = entry.getKey();

            Class<?> type = key.getLeft();
            Annotation[] annotations = key.getRight();
            String name = type.getName();
            Label continuer = new Label();
            boolean isIndex = Arrays.stream(annotations).anyMatch(p -> p instanceof IsIndex);
            // LuaValue -> anything
            switch (name) {
                case "java.lang.Void", "void" -> {
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Z
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "isnil",
                            "()Z",
                            false
                    );
                    // LuaValue Z -> LuaValue
                    visitor.visitJumpInsn(IFEQ, continuer);
                    // LuaValue ->
                    visitor.visitInsn(POP);
                    // -> null
                    visitor.visitInsn(ACONST_NULL);
                }
                case "java.lang.String" -> {
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Z
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "isstring",
                            "()Z",
                            false
                    );
                    // LuaValue Z -> LuaValue
                    visitor.visitJumpInsn(IFEQ, continuer);
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Z
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "isinttype",
                            "()Z",
                            false
                    );
                    // LuaValue Z -> LuaValue
                    visitor.visitJumpInsn(IFNE, continuer);
                    // LuaValue -> String
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "checkjstring",
                            "()Ljava/lang/String;",
                            false
                    );
                }
                case "java.lang.Integer", "int" -> {
                    extractPrimitiveFromArgument(visitor, "isinttype", continuer, "checkint", "I");
                    subtractPrimitiveIfIndex(visitor, isIndex, 1, ISUB);
                    upgradeToBoxIfAvailable(visitor, Integer.class, int.class, name);
                }
                case "java.lang.Boolean", "bool" -> {
                    extractPrimitiveFromArgument(visitor, "isboolean", continuer, "checkboolean", "Z");
                    upgradeToBoxIfAvailable(visitor, Boolean.class, boolean.class, name);
                }
                case "java.lang.Byte", "byte" -> {
                    extractPrimitiveFromArgument(visitor, "isinttype", continuer, "checkint", "I");
                    visitor.visitInsn(I2B);
                    subtractPrimitiveIfIndex(visitor, isIndex, (byte) 1, ISUB);
                    upgradeToBoxIfAvailable(visitor, Byte.class, byte.class, name);
                }
                case "java.lang.Character", "char" -> {
                    extractPrimitiveFromArgument(visitor, "isinttype", continuer, "checkint", "I");
                    visitor.visitInsn(I2C);
                    subtractPrimitiveIfIndex(visitor, isIndex, (char) 1, ISUB);
                    upgradeToBoxIfAvailable(visitor, Character.class, char.class, name);
                }
                case "java.lang.Short", "short" -> {
                    extractPrimitiveFromArgument(visitor, "isinttype", continuer, "checkint", "I");
                    visitor.visitInsn(I2S);
                    subtractPrimitiveIfIndex(visitor, isIndex, (short) 1, ISUB);
                    upgradeToBoxIfAvailable(visitor, Short.class, short.class, name);
                }
                case "java.lang.Double", "double" -> {
                    extractPrimitiveFromArgument(visitor, "isinttype", continuer, "checkdouble", "D");
                    subtractPrimitiveIfIndex(visitor, isIndex, 1d, DSUB);
                    upgradeToBoxIfAvailable(visitor, Double.class, double.class, name);
                }
                case "java.lang.Float", "float" -> {
                    extractPrimitiveFromArgument(visitor, "isinttype", continuer, "checkdouble", "D");
                    visitor.visitInsn(D2F);
                    subtractPrimitiveIfIndex(visitor, isIndex, 1f, FSUB);
                    upgradeToBoxIfAvailable(visitor, Float.class, float.class, name);
                }
                case "java.lang.Long", "long" -> {
                    extractPrimitiveFromArgument(visitor, "isinttype", continuer, "checklong", "J");
                    subtractPrimitiveIfIndex(visitor, isIndex, 1L, LSUB);
                    upgradeToBoxIfAvailable(visitor, Long.class, long.class, name);
                }
                case "org.luaj.vm2.LuaTable" -> {
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Z
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "istable",
                            "()Z",
                            false
                    );
                    // LuaValue Z -> LuaValue
                    visitor.visitJumpInsn(IFEQ, continuer);
                    // LuaValue -> LuaTable
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "checktable",
                            "()Lorg/luaj/vm2/LuaTable;",
                            false
                    );
                }
                case "org.luaj.vm2.LuaFunction" -> {
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Z
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "isfunction",
                            "()Z",
                            false
                    );
                    // LuaValue Z -> LuaValue
                    visitor.visitJumpInsn(IFEQ, continuer);
                    // LuaValue -> LuaFunction
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "checkfunction",
                            "()Lorg/luaj/vm2/LuaFunction;",
                            false
                    );
                }
                default -> {
                    continue;
                }
            }

            iterator.remove();

            // anything -> anything
            if (!entry.getValue().getAsBoolean()) {
                // anything ->
                visitor.visitInsn(Type.getType(type).getSize() == 2 ? POP2 : POP);
                // -> LuaValue
                luaValuePusher.run();
            }

            visitor.visitLabel(continuer);
        }

        for (Map.Entry<Pair<Class<?>, Annotation[]>, BooleanSupplier> entry : leftover) {
            Pair<Class<?>, Annotation[]> key = entry.getKey();
            Class<?> type = key.getLeft();
            Annotation[] annotations = key.getRight();
            BooleanSupplier runnable = entry.getValue();
            Label continuer = new Label();
            // LuaValue -> anything
            String name = type.getName();
            switch (name) {
                case "org.luaj.vm2.LuaValue", "org.luaj.vm2.Varargs" -> {
                }
                case "java.lang.Object" -> {
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Z
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "isuserdata",
                            "()Z",
                            false
                    );
                    // LuaValue Z -> LuaValue
                    visitor.visitJumpInsn(IFEQ, continuer);
                    // LuaValue -> Object
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "checkuserdata",
                            "()Ljava/lang/Object;",
                            false
                    );
                }
                default -> {
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Z
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "isuserdata",
                            "()Z",
                            false
                    );
                    // LuaValue Z -> LuaValue
                    visitor.visitJumpInsn(IFEQ, continuer);
                    // LuaValue -> LuaValue LuaValue
                    visitor.visitInsn(DUP);
                    // LuaValue LuaValue -> LuaValue Object
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Consts.LUA_VALUE.getInternalName(),
                            "checkuserdata",
                            "()Ljava/lang/Object;",
                            false
                    );
                    // LuaValue Object -> LuaValue Object Object
                    visitor.visitInsn(DUP);
                    // LuaValue Object Object -> LuaValue Object Z
                    visitor.visitTypeInsn(INSTANCEOF, name.replace('.', '/'));

                    Label t = new Label();
                    Label c = new Label();
                    // LuaValue Object Z -> LuaValue Object
                    visitor.visitJumpInsn(IFEQ, t);
                    visitor.visitJumpInsn(GOTO, c);
                    visitor.visitLabel(t);
                    // LuaValue Object -> LuaValue
                    visitor.visitInsn(POP);
                    visitor.visitJumpInsn(GOTO, continuer);
                    visitor.visitLabel(c);
                    // LuaValue Object -> LuaValue specified
                    visitor.visitTypeInsn(CHECKCAST, name.replace('.', '/'));
                    // LuaValue specified -> specified LuaValue specified
                    visitor.visitInsn(DUP_X1);
                    // specified LuaValue specified -> specified LuaValue
                    visitor.visitInsn(POP);
                    // specified LuaValue -> specified
                    visitor.visitInsn(POP);
                }
            }

            // anything -> anything
            if (!runnable.getAsBoolean()) {
                // anything ->
                visitor.visitInsn(Type.getType(type).getSize() == 2 ? POP2 : POP);
                // -> LuaValue
                luaValuePusher.run();
            }

            visitor.visitLabel(continuer);
        }
    }

    private void compileActualCall(MethodVisitor visitor, List<String> previous, ASMDispatchGenerator.Executor executor) {
        // -> Bridge
        visitor.visitVarInsn(ALOAD, 2);
        // Bridge -> Bridge this
        visitor.visitVarInsn(ALOAD, 0);
        // Bridge this -> this Bridge this
        visitor.visitInsn(DUP_X1);
        // this Bridge this -> this Bridge String
        visitor.visitFieldInsn(
                GETFIELD,
                Consts.SURROGATE_CLASS_NAME.getInternalName(),
                "methodName",
                Type.getDescriptor(String.class)
        );
        // this Bridge String -> this Bridge String
        ASMUtil.emitStringConcat(visitor, new Type[]{Type.getType(String.class)}, true, "(" + String.join(", ", previous) + ")");
        // this Bridge String ->
        visitor.visitMethodInsn(
                INVOKEVIRTUAL,
                Consts.SURROGATE_CLASS_NAME.getInternalName(),
                "callResolved",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OverloadedMethodHook.class), Type.getType(String.class)),
                false
        );

        // -> thing (remember that the previous arguments are still there)
        executor.executor().apply(visitor);

        // thing -> Varargs
        compileReturnValue(
                visitor,
                executor.returnType(),
                executor.returnAnnotations()
        );

        // Varargs -> Varargs
        emitExitHook(visitor);
        visitor.visitInsn(ARETURN);
    }

    private GeneratedWrap compile(ASMDispatchGenerator.Node rootNode, int handleCount) {
        classWriter.visit(
                V1_8,
                ACC_PUBLIC,
                Consts.SURROGATE_CLASS_NAME.getInternalName(),
                null,
                "java/lang/Object",
                new String[]{Type.getType(DispatchGenerator.Dispatch.class).getInternalName()}
        );

        compileFields();

        MethodVisitor method = classWriter.visitMethod(
                ACC_PUBLIC,
                "dispatch",
                Type.getMethodDescriptor(
                        Type.getType(Varargs.class),
                        Type.getType(LuaRuntime.class),
                        Type.getType(Bridge.class),
                        Type.getType(Varargs.class)
                ),
                null,
                null
        );

        compileMainMethod(method, rootNode);

        method.visitMaxs(0, 0);

        method.visitEnd();

        compileInit();

        method = classWriter.visitMethod(
                ACC_PUBLIC,
                "callResolved",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OverloadedMethodHook.class), Type.getType(String.class)),
                null, null
        );

        method.visitCode();
        // -> Hooks
        method.visitVarInsn(ALOAD, 1);
        // Hooks -> Hooks String
        method.visitVarInsn(ALOAD, 2);
        // Hooks String ->
        MethodVisitor finalMethod = method;
        ASMUtil.emitTryCatch(method, () -> {
            // Hooks String ->
            finalMethod.visitMethodInsn(
                    INVOKEINTERFACE,
                    Consts.HOOK_NAME.getInternalName(),
                    "enterResolved",
                    "(Ljava/lang/String;)V",
                    true
            );
        }).emitCatch(Type.getType(Exception.class), () -> {
            emitIgnoreError(finalMethod, "Ignoring error during callResolved hook");
        }).emit();
        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        classWriter.visitEnd();
        byte[] c;
        compiledClass = c = classWriter.toByteArray();

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandles.Lookup childLookup;
        try {
            childLookup = lookup.defineHiddenClass(c, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Class<?> aClass = childLookup.lookupClass();
        try {
            return new GeneratedWrap(lookup.findConstructor(aClass, MethodType.methodType(void.class, String.class, MethodHandle[].class)), handleCount);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void compileFields() {
        classWriter.visitField(
                ACC_PRIVATE | ACC_FINAL,
                "handles",
                Type.getDescriptor(MethodHandle[].class),
                null,
                null
        );

        classWriter.visitField(
                ACC_PRIVATE | ACC_FINAL,
                "methodName",
                Type.getDescriptor(String.class),
                null,
                null
        );
    }

    private void compileInit() {
        MethodVisitor method = classWriter.visitMethod(
                ACC_PUBLIC,
                "<init>",
                Type.getMethodDescriptor(
                        Type.VOID_TYPE,
                        Type.getType(String.class),
                        Type.getType(MethodHandle[].class)
                ),
                null,
                null
        );

        method.visitCode();

        // super
        method.visitVarInsn(ALOAD, 0);
        method.visitInsn(DUP);
        method.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        method.visitVarInsn(ALOAD, 0);
        method.visitVarInsn(ALOAD, 1);
        method.visitFieldInsn(PUTFIELD,
                Consts.SURROGATE_CLASS_NAME.getInternalName(),
                "methodName",
                Type.getDescriptor(String.class)
        );

        method.visitVarInsn(ALOAD, 0);
        method.visitVarInsn(ALOAD, 2);
        method.visitFieldInsn(PUTFIELD,
                Consts.SURROGATE_CLASS_NAME.getInternalName(),
                "handles",
                Type.getDescriptor(MethodHandle[].class)
        );
        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);

        method.visitEnd();
    }

    private void compileMainMethod(MethodVisitor visitor, ASMDispatchGenerator.Node rootNode) {
        // -> bridge
        visitor.visitVarInsn(ALOAD, 2);
        // bridge -> bridge varargs
        visitor.visitVarInsn(ALOAD, 3);

        // bridge varargs ->
        ASMUtil.emitTryCatch(visitor, () -> {
            // bridge varargs ->
            visitor.visitMethodInsn(
                    INVOKEINTERFACE,
                    Consts.HOOK_NAME.getInternalName(),
                    "enter",
                    "(Lorg/luaj/vm2/Varargs;)V",
                    true
            );
        }).emitCatch(Type.getType(Exception.class), () -> emitIgnoreError(visitor, "Ignoring error during enter hook")).emit();
        visitor.visitInsn(ICONST_M1);
        visitor.visitVarInsn(ISTORE, 4);
        visitor.visitLdcInsn("");
        visitor.visitVarInsn(ASTORE, 5);
        visitor.visitLdcInsn("");
        visitor.visitVarInsn(ASTORE, 6);
        visitor.visitLdcInsn("");
        visitor.visitVarInsn(ASTORE, 7);
        ASMUtil.emitTryCatch(visitor, () -> {
                    compileNode(visitor, new ArrayList<>(), rootNode, 1);
                    // -> String
                    visitor.visitVarInsn(ALOAD, 5);
                    // String -> String String
                    visitor.visitInsn(DUP);
                    // String String -> String I
                    visitor.visitMethodInsn(
                            INVOKEVIRTUAL,
                            Type.getInternalName(String.class),
                            "isEmpty",
                            "()Z",
                            false
                    );
                    Label ifFalse = new Label();
                    Label after = new Label();
                    // String I -> String
                    visitor.visitJumpInsn(IFNE, ifFalse);
                    // String -> String
                    ASMUtil.emitStringConcat(visitor, new Type[]{Type.getType(String.class)}, false, " while figuring out (", ", ____)");
                    visitor.visitJumpInsn(GOTO, after);
                    visitor.visitLabel(ifFalse);
                    // String ->
                    visitor.visitInsn(POP);
                    // -> String
                    visitor.visitLdcInsn(" from the first argument");
                    visitor.visitLabel(after);

                    // String -> String String
                    visitor.visitVarInsn(ALOAD, 6);
                    // String String -> String String String
                    visitor.visitVarInsn(ALOAD, 7);
                    // String String String -> String
                    ASMUtil.emitStringConcat(
                            visitor,
                            new Type[]{Type.getType(String.class), Type.getType(String.class), Type.getType(String.class)},
                            false,
                            "Unknown overload", " because ", ", expected "
                    );
                    // String -> String Class
                    visitor.visitTypeInsn(NEW, "org/luaj/vm2/LuaError");
                    // String Class -> Class String Class
                    visitor.visitInsn(DUP_X1);
                    // Class String Class -> Class Class String Class
                    visitor.visitInsn(DUP_X1);
                    // Class String Class -> Class Class String
                    visitor.visitInsn(POP);
                    // Class Class String -> IAE
                    visitor.visitMethodInsn(
                            INVOKESPECIAL,
                            "org/luaj/vm2/LuaError",
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false
                    );
                    visitor.visitInsn(ATHROW);
                })
                .emitCatch(Type.getType(Throwable.class), () -> {
                    emitErrorHook(visitor);
                    visitor.visitInsn(ATHROW);
                })
                .emit();

    }

    private void compileNode(MethodVisitor visitor, List<String> previous, ASMDispatchGenerator.Node node, int varargsPos) {
        Label terminator = new Label();
        for (Map.Entry<Class<?>, ArrayList<ASMDispatchGenerator.ParamNode>> entry : node.types.entrySet()) {
            Class<?> key = entry.getKey();

            if (key.isPrimitive()) {
                continue;
            }

            CTMethodArgumentResolverHandler handler = generator.argumentResolvers.findFirst(key);
            if (handler != null) {
                ArgumentResolverContext context = new ArgumentResolverContext(visitor, classWriter, generator);
                for (ASMDispatchGenerator.ParamNode paramNode : entry.getValue()) {
                    // -> thing
                    handler.resolve(key, paramNode, context);
                    compileNode(visitor, previous, paramNode.node, varargsPos);
                    // thing ->
                    visitor.visitInsn(Type.getType(key).getSize() == 1 ? POP : POP2);
                }
            }
        }

        Map<Pair<Class<?>, Annotation[]>, ASMDispatchGenerator.Executor> execute = node.executorsByVarargs;

        Label pastCallReturn = new Label();
        execution:
        {
            Label afterNoVararg = new Label();
            if (execute.containsKey(null) || execute.isEmpty()) {
                // -> varargs
                visitor.visitVarInsn(ALOAD, 3);

                // varargs -> I
                visitor.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "org/luaj/vm2/Varargs",
                        "narg",
                        "()I",
                        false
                );
                // I -> I I
                visitor.visitLdcInsn(varargsPos - 1);
                // I I ->
                visitor.visitJumpInsn(IF_ICMPNE, afterNoVararg);

                if (execute.isEmpty()) {
                    overloadFollowFail(visitor, varargsPos, node, () -> {
                        // -> String
                        visitor.visitLdcInsn(varargsPos == 1 ? "there isn't an overload that takes no argument" : "there are no more arguments and this isn't a valid overload");
                        // String ->
                        visitor.visitVarInsn(ASTORE, 6);

                        // -> String
                        visitor.visitLdcInsn(String.join(", ", previous));
                        // String ->
                        visitor.visitVarInsn(ASTORE, 5);
                    }, () -> {
                    });
                    visitor.visitJumpInsn(GOTO, terminator);
                    visitor.visitLabel(afterNoVararg);
                    break execution;
                } else {
                    ASMDispatchGenerator.Executor executor = execute.get(null);
                    compileActualCall(visitor, previous, executor);
                }
            }

            visitor.visitLabel(afterNoVararg);

            for (Map.Entry<Pair<Class<?>, Annotation[]>, ASMDispatchGenerator.Executor> entry : execute.entrySet()) {
                if (entry.getKey() == null) continue;
                Label afterVarargs = new Label();
                Class<?> arityArgument = entry.getKey().getLeft();
                Annotation[] arityAnnotations = entry.getKey().getRight();
                ASMDispatchGenerator.Executor executor = entry.getValue();
                // -> pos
                visitor.visitLdcInsn(varargsPos);
                // pos -> pos pos
                visitor.visitInsn(DUP);
                // pos pos -> pos pos varargs
                visitor.visitVarInsn(ALOAD, 3);

                // pos pos varargs -> pos pos size
                visitor.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "org/luaj/vm2/Varargs",
                        "narg",
                        "()I",
                        false
                );

                // pos pos size -> pos size pos size
                visitor.visitInsn(DUP_X1);
                // pos size pos size -> pos size size pos
                visitor.visitInsn(SWAP);

                visitor.visitInsn(ICONST_1);
                visitor.visitInsn(ISUB);


                // pos size size pos -> pos size size-pos
                visitor.visitInsn(ISUB);
                // pos size size-pos -> pos size thing[]
                if (arityArgument.isPrimitive() && arityArgument != void.class) {
                    visitor.visitIntInsn(NEWARRAY, TypeInfo.ARRAY_ATYPE[TypeInfo.find(TypeInfo.PRIMITIVES, arityArgument)]);
                } else {
                    visitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(arityArgument));
                }
                // pos size thing[] -> pos thing[] size
                visitor.visitInsn(SWAP);
                // pos thing[] size -> thing[] size pos thing[] size
                visitor.visitInsn(DUP2_X1);
                // thing[] size pos thing[] size -> thing[] size pos
                visitor.visitInsn(POP2);

                Label loopStart = new Label();
                Label term = new Label();

                visitor.visitLabel(loopStart);

                // thing[] max index -> thing[] max index max index
                visitor.visitInsn(DUP2);
                // thing[] max index max index -> thing[] max index
                visitor.visitJumpInsn(IF_ICMPLT, term);
                // thing[] max index -> thing[] max index index
                visitor.visitInsn(DUP);
                // thing[] max index index -> thing[] max index index varargs
                visitor.visitVarInsn(ALOAD, 3);
                // thing[] max index index varargs -> thing[] max index varargs index
                visitor.visitInsn(SWAP);
                // thing[] max index varargs index -> thing[] max index LuaValue
                visitor.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "org/luaj/vm2/Varargs",
                        "arg",
                        "(I)Lorg/luaj/vm2/LuaValue;",
                        false
                );
                // thing[] max index LuaValue -> thing[] index LuaValue max index LuaValue
                visitor.visitInsn(DUP2_X1);
                // thing[] index LuaValue max index LuaValue -> thing[] index LuaValue max
                visitor.visitInsn(POP2);
                // thing[] index LuaValue max -> LuaValue max thing[] index LuaValue max
                visitor.visitInsn(DUP2_X2);
                // LuaValue max thing[] index LuaValue max -> LuaValue max thing[] index LuaValue
                visitor.visitInsn(POP);
                // LuaValue max thing[] index LuaValue -> LuaValue max LuaValue thing[] index LuaValue
                visitor.visitInsn(DUP_X2);
                // LuaValue max LuaValue thing[] index LuaValue -> LuaValue max LuaValue thing[] index
                visitor.visitInsn(POP);
                // LuaValue max LuaValue thing[] index -> LuaValue max thing[] index LuaValue thing[] index
                visitor.visitInsn(DUP2_X1);

                // LuaValue max thing[] index LuaValue thing[] index -> LuaValue max thing[] index LuaValue thing[] array-index
                visitor.visitLdcInsn(varargsPos);
                visitor.visitInsn(ISUB);

                // LuaValue max thing[] index LuaValue thing[] array-index -> LuaValue max thing[] index thing[] array-index LuaValue thing[] array-index
                visitor.visitInsn(DUP2_X1);
                // LuaValue max thing[] index thing[] array-index LuaValue thing[] array-index -> LuaValue max thing[] index thing[] array-index LuaValue
                visitor.visitInsn(POP2);

                // LuaValue max thing[] index thing[] array-index LuaValue -> LuaValue max thing[] index thing[] array-index LuaValue LuaValue
                visitor.visitInsn(DUP);

                Label success = new Label();
                Map<Pair<Class<?>, Annotation[]>, BooleanSupplier> types = new HashMap<>();
                types.put(Pair.of(arityArgument, arityAnnotations), () -> {
                    // LuaValue max thing[] index thing[] array-index LuaValue specified
                    visitor.visitJumpInsn(GOTO, success);
                    return true;
                });
                compileLuaToJavaDecoding(visitor, () -> visitor.visitInsn(DUP), types);

                // LuaValue max thing[] index thing[] array-index LuaValue LuaValue -> LuaValue max thing[] index thing[] LuaValue array-index LuaValue LuaValue
                visitor.visitInsn(DUP_X2);
                // LuaValue max thing[] index thing[] LuaValue array-index LuaValue LuaValue -> LuaValue max thing[] index thing[] LuaValue array-index
                visitor.visitInsn(POP2);
                // LuaValue max thing[] index thing[] LuaValue array-index -> LuaValue max thing[] index thing[] LuaValue
                visitor.visitInsn(POP);
                // LuaValue max thing[] index thing[] LuaValue -> LuaValue max thing[] LuaValue index thing[] LuaValue
                visitor.visitInsn(DUP_X2);
                // LuaValue max thing[] LuaValue index thing[] LuaValue -> LuaValue max thing[] LuaValue index
                visitor.visitInsn(POP2);

                // LuaValue max thing[] LuaValue index -> LuaValue max thing[] LuaValue index index
                visitor.visitInsn(DUP);

                // LuaValue max thing[] LuaValue index index -> LuaValue max thing[]
                overloadFollowFail(visitor, node, () -> {
                    // ... LuaValue index

                    // LuaValue index -> index LuaValue
                    visitor.visitInsn(SWAP);

                    // index LuaValue -> index String
                    luaValueToDebugName(visitor);

                    // index String -> String index
                    visitor.visitInsn(SWAP);

                    // String index -> String
                    ASMUtil.emitStringConcat(visitor, new Type[]{Type.getType(String.class), Type.INT_TYPE}, false, "not all varargs arguments convert to " + arityArgument.getSimpleName() + ", found ", " at position ");
                    // String ->
                    visitor.visitVarInsn(ASTORE, 6);

                    // -> String
                    visitor.visitLdcInsn(String.join(", ", previous));
                    // String ->
                    visitor.visitVarInsn(ASTORE, 5);
                }, () -> visitor.visitInsn(POP2));

                // LuaValue max thing[] -> LuaValue max
                visitor.visitInsn(POP);
                // LuaValue max ->
                visitor.visitInsn(POP2);

                visitor.visitJumpInsn(GOTO, afterVarargs);

                // LuaValue max thing[] index thing[] array-index LuaValue specified
                visitor.visitLabel(success);

                // LuaValue max thing[] index thing[] array-index LuaValue specified -> LuaValue max thing[] index thing[] array-index specified
                if (Type.getType(arityArgument).getSize() == 2) {
                    visitor.visitInsn(DUP2_X1);
                    visitor.visitInsn(POP2);
                    visitor.visitInsn(POP);
                } else {
                    visitor.visitInsn(SWAP);
                    visitor.visitInsn(POP);
                }

                // LuaValue max thing[] index thing[] array-index thing -> LuaValue max thing[] index
                int opcode = Type.getType(arityArgument).getOpcode(IASTORE);
                visitor.visitInsn(opcode);

                // LuaValue max thing[] index -> LuaValue max thing[] index I
                visitor.visitInsn(ICONST_1);
                // LuaValue max thing[] index I -> LuaValue max thing[] index
                visitor.visitInsn(IADD);
                // LuaValue max thing[] index -> thing[] index LuaValue max thing[] index
                visitor.visitInsn(DUP2_X2);
                // thing[] index LuaValue max thing[] index -> thing[] index LuaValue max
                visitor.visitInsn(POP2);
                // thing[] index LuaValue max -> thing[] max index LuaValue max
                visitor.visitInsn(DUP_X2);
                // thing[] max index LuaValue max -> thing[] max index
                visitor.visitInsn(POP2);


                // thing[] max index
                visitor.visitJumpInsn(GOTO, loopStart);

                visitor.visitLabel(term);
                // thing[] max index -> thing[]
                visitor.visitInsn(POP2);

                previous.add(arityArgument.getSimpleName() + "...");
                compileActualCall(visitor, previous, executor);
                previous.remove(previous.size() - 1);

                visitor.visitLabel(afterVarargs);
            }


        }
        visitor.visitLabel(pastCallReturn);

        if (node.ifNull == null && node.types.isEmpty() && node.executorsByVarargs.size() == 1) {
            overloadFollowFail(visitor, varargsPos, node, () -> {
                // -> String
                visitor.visitLdcInsn("there is nothing that can possibly take more arguments");
                // String ->
                visitor.visitVarInsn(ASTORE, 6);

                // -> String
                visitor.visitLdcInsn(String.join(", ", previous));
                // String ->
                visitor.visitVarInsn(ASTORE, 5);
            }, () -> {
            });
            visitor.visitLabel(terminator);
            return;
        }


        // -> LuaValue
        visitor.visitVarInsn(ALOAD, 3);
        pushLuaValue(visitor, varargsPos);

        Map<Pair<Class<?>, Annotation[]>, BooleanSupplier> types = new HashMap<>();
        types.put(Pair.of(void.class, new Annotation[0]), () -> {
            // null
            if (node.ifNull == null) {
                overloadFollowFail(visitor, varargsPos, node, () -> {
                    // -> String
                    visitor.visitLdcInsn("the argument can't be nil");
                    // String ->
                    visitor.visitVarInsn(ASTORE, 6);

                    // -> String
                    visitor.visitLdcInsn(String.join(", ", previous));
                    // String ->
                    visitor.visitVarInsn(ASTORE, 5);
                }, () -> {
                });
            } else {
                // null -> null
                previous.add("null");
                compileNode(visitor, previous, node.ifNull, varargsPos + 1);
                previous.remove(previous.size() - 1);
            }
            return false;
        });

        for (Map.Entry<Class<?>, ArrayList<ASMDispatchGenerator.ParamNode>> entry : node.types.entrySet()) {
            for (ASMDispatchGenerator.ParamNode paramNode : entry.getValue()) {
                types.put(Pair.of(entry.getKey(), paramNode.settings), () -> {
                    previous.add(entry.getKey().getSimpleName());
                    compileNode(visitor, previous, paramNode.node, varargsPos + 1);
                    previous.remove(previous.size() - 1);
                    return false;
                });
            }
        }

        compileLuaToJavaDecoding(visitor, () -> {
            // -> varargs
            visitor.visitVarInsn(ALOAD, 3);
            // varargs -> LuaValue
            pushLuaValue(visitor, varargsPos);
        }, types);

        // LuaValue ->
        overloadFollowFail(visitor, varargsPos, node, () -> {
            String precede = "found ";
            // LuaValue -> String
            luaValueToDebugName(visitor);
            // String -> String
            ASMUtil.emitStringConcat(visitor, new Type[]{Type.getType(String.class)}, false, precede, ".");
            // String ->
            visitor.visitVarInsn(ASTORE, 6);

            // -> String
            visitor.visitLdcInsn(String.join(", ", previous));
            // String ->
            visitor.visitVarInsn(ASTORE, 5);
        }, () -> visitor.visitInsn(POP));
        visitor.visitLabel(terminator);
    }

    // Throwable ->
    private void emitIgnoreError(MethodVisitor visitor, String ignoreMessage) {

        // Throwable -> Throwable Logger
        visitor.visitFieldInsn(
                GETSTATIC,
                Consts.OWNER_NAME.getInternalName(),
                "logger",
                Type.getDescriptor(Logger.class)
        );
        // Throwable Logger -> Logger Throwable Logger
        visitor.visitInsn(DUP_X1);
        // Logger Throwable Logger -> Logger Throwable
        visitor.visitInsn(POP);

        // Logger Throwable -> Logger Throwable String
        visitor.visitLdcInsn(ignoreMessage);

        // Logger Throwable String -> Logger String Throwable String
        visitor.visitInsn(DUP_X1);
        // Logger String Throwable String -> Logger String Throwable
        visitor.visitInsn(POP);
        // Logger String Throwable ->
        visitor.visitMethodInsn(
                INVOKEINTERFACE,
                Type.getInternalName(Logger.class),
                "error",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(Object.class)),
                true
        );
    }

    private String toSimpleName(Type type) {
        String name = type.getClassName();
        return name.substring(Math.max(name.lastIndexOf('.') + 1, 0));
    }

    GeneratedWrap getAssigner() {
        return compiled;
    }

    static class GeneratedWrap implements BiFunction<String, MethodHandle[], DispatchGenerator.Dispatch> {
        MethodHandle type;
        int handleCount;

        GeneratedWrap(MethodHandle type, int handleCount) {
            this.type = type.asType(type.type().changeReturnType(DispatchGenerator.Dispatch.class));
            this.handleCount = handleCount;

        }

        @Override
        public DispatchGenerator.Dispatch apply(String name, MethodHandle[] methodHandles) {
            if (handleCount != methodHandles.length) {
                throw new IllegalArgumentException("Incorrect amount of handles");
            }
            try {
                return (DispatchGenerator.Dispatch) type.invokeExact(name, methodHandles);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
