package com.github.applejuiceyy.luabridge.asm;

import com.github.applejuiceyy.luabridge.ClassMap;
import com.github.applejuiceyy.luabridge.DispatchGenerator;
import com.github.applejuiceyy.luabridge.OverloadedMethod;
import com.github.applejuiceyy.luabridge.annotation.IsNullable;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class ASMDispatchGenerator implements DispatchGenerator {
    // synchronized to java.lang.invoke.MethodHandleInfo's constants
    private static final int[] FIELD_OPCODE = new int[]{GETFIELD, PUTFIELD, GETSTATIC, PUTSTATIC};

    final ClassMap<CTMethodArgumentConverterHandler> argumentConverters;
    final ClassMap<CTMethodArgumentResolverHandler> argumentResolvers;
    private final HashMap<ImmutableList<HashCodeableOverload>, BiFunction<String, MethodHandle[], Dispatch>> cache = new HashMap<>();


    public ASMDispatchGenerator(ClassMap<CTMethodArgumentConverterHandler> argumentConverters, ClassMap<CTMethodArgumentResolverHandler> argumentResolvers) {
        this.argumentConverters = argumentConverters;
        this.argumentResolvers = argumentResolvers;
    }

    public static ASMDispatchGeneratorBuilder create() {
        return new ASMDispatchGeneratorBuilder();
    }

    @Override
    public Dispatch generateDispatch(OverloadedMethod overloadedMethod, ImmutableList<OverloadedMethod.Overload> overloads) {
        List<HashCodeableOverload> appliers = new ArrayList<>();
        for (int i = 0, overloadsSize = overloads.size(); i < overloadsSize; i++) {
            OverloadedMethod.Overload overload = overloads.get(i);
            MethodHandle handle = overload.handle();
            BytecodeCallerApplier applier = crackMethodHandle(handle);
            if (applier == null) {
                applier = new MethodHandleExecutor(handle.type(), i);
            }
            appliers.add(
                    new HashCodeableOverload(
                            handle.type(),
                            applier,
                            ImmutableList.copyOf(
                                    Arrays.stream(overload.argumentAnnotations())
                                            .map(ImmutableList::copyOf)
                                            .iterator()
                            ),
                            ImmutableList.copyOf(overload.returnAnnotation()),
                            overload.handle().isVarargsCollector()
                    )
            );
        }


        ImmutableList<HashCodeableOverload> hashable = appliers.stream().collect(ImmutableList.toImmutableList());

        if (!cache.containsKey(hashable)) {
            Node node = new Node();
            for (HashCodeableOverload overload : appliers) {
                treeify(
                        overload.applier(),
                        overload.type.parameterArray(),
                        overload.parameters.stream().map(l -> l.toArray(Annotation[]::new)).toArray(Annotation[][]::new),
                        overload.type.returnType(),
                        overload.returnType.toArray(Annotation[]::new),
                        overload.variableArity,
                        node,
                        0
                );
            }
            cache.put(hashable, new ClassGenerator(this, node, overloads.size()).getAssigner());
        }

        MethodHandle[] array = overloads.stream().map(OverloadedMethod.Overload::handle).toArray(MethodHandle[]::new);
        return cache.get(hashable).apply(overloadedMethod.getName(), array);
    }

    private @Nullable CrackedElementExecutor crackMethodHandle(MethodHandle methodHandle) {
        MethodHandleInfo handleInfo;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType type = methodHandle.type();
        try {
            handleInfo = lookup.revealDirect(methodHandle);
            int referenceKind = handleInfo.getReferenceKind();

            // behaviour taken from java.lang.invoke.InfoFromMemberName.reflectUnchecked
            if (referenceKind <= MethodHandleInfo.REF_putStatic) {
                Field field = handleInfo.reflectAs(Field.class, lookup);
                return new CrackedElementExecutor(field, visitor -> {
                    visitor.visitFieldInsn(
                            FIELD_OPCODE[referenceKind - 1],
                            Type.getInternalName(field.getDeclaringClass()),
                            field.getName(),
                            Type.getDescriptor(field.getType())
                    );
                });
            } else if (referenceKind == MethodHandleInfo.REF_newInvokeSpecial) {
                Constructor<?> constructor = handleInfo.reflectAs(Constructor.class, lookup);
                return new CrackedElementExecutor(constructor, visitor -> {
                    String declarer = Type.getInternalName(constructor.getDeclaringClass());
                    visitor.visitTypeInsn(
                            NEW,
                            declarer
                    );
                    visitor.visitMethodInsn(
                            INVOKESPECIAL,
                            declarer,
                            "<init>",
                            Type.getMethodDescriptor(
                                    Type.getType(type.returnType()),
                                    Arrays.stream(type.parameterArray()).map(Type::getType).toArray(Type[]::new)
                            ),
                            false
                    );
                });
            } else {
                Method method = handleInfo.reflectAs(Method.class, lookup);

                return new CrackedElementExecutor(method, visitor -> {
                    // done inside because it's only done once
                    int opcode;
                    boolean i = false;
                    if (Modifier.isStatic(method.getModifiers())) {
                        opcode = INVOKESTATIC;
                    } else {
                        Boolean b = collectInterfaces(method.getDeclaringClass(), c -> {
                            try {
                                c.getDeclaredMethod(method.getName(), method.getParameterTypes());
                            } catch (NoSuchMethodException e) {
                                return null;
                            }
                            return true;
                        });
                        if (b != null) {
                            opcode = INVOKEINTERFACE;
                            i = true;
                        } else {
                            opcode = INVOKEVIRTUAL;
                        }
                    }

                    visitor.visitMethodInsn(
                            opcode,
                            Type.getInternalName(method.getDeclaringClass()),
                            method.getName(),
                            Type.getMethodDescriptor(
                                    Type.getType(type.returnType()),
                                    Arrays.stream(type.parameterArray()).map(Type::getType).skip(opcode == INVOKESTATIC ? 0 : 1).toArray(Type[]::new)
                            ),
                            i
                    );
                });
            }
        } catch (IllegalArgumentException arg) {
            return null;
        }
    }

    private <T> T collectInterfaces(Class<?> type, Function<Class<?>, @Nullable T> interfaceConsumer) {
        Class<?> superclass = type.getSuperclass();
        if (superclass != null) {
            T applied = collectInterfaces(superclass, interfaceConsumer);
            if (applied != null) return applied;
        }
        for (Class<?> anInterface : type.getInterfaces()) {
            T applied = interfaceConsumer.apply(anInterface);
            if (applied != null) return applied;
            // interfaces may extend interfaces
            collectInterfaces(anInterface, interfaceConsumer);
        }
        return null;
    }

    private void treeify(BytecodeCallerApplier handle, Class<?>[] paramTypes, Annotation[][] paramAnnotations, Class<?> returnType, Annotation[] returnAnnotations, boolean variableArity, Node node, int arg) {
        boolean addExecutor = false;
        Pair<Class<?>, Annotation[]> varargsType = null;
        if (variableArity) {
            if (arg == paramTypes.length - 1) {
                addExecutor = true;
                varargsType = Pair.of(paramTypes[arg].componentType(), paramAnnotations[arg]);
            }
        } else {
            if (arg == paramTypes.length) {
                addExecutor = true;
            }
        }
        if (addExecutor) {
            if (node.executorsByVarargs.containsKey(varargsType)) {
                throw new IllegalStateException("Same overload");
            }
            node.executorsByVarargs.put(varargsType, new Executor(handle, returnType, returnAnnotations));
            return;
        }

        Annotation[] parameterAnnotations = paramAnnotations[arg];
        Optional<Annotation> parameter = Arrays.stream(parameterAnnotations).filter(a -> a instanceof IsNullable).findFirst();
        Class<?> parameterType = paramTypes[arg];
        boolean isVoid = parameterType == Void.class;
        if (parameter.isPresent() || isVoid) {
            if (node.ifNull != null) {
                treeify(handle, paramTypes, paramAnnotations, returnType, returnAnnotations, variableArity, node.ifNull, arg + 1);
            } else {
                Node ifN = new Node();
                treeify(handle, paramTypes, paramAnnotations, returnType, returnAnnotations, variableArity, ifN, arg + 1);
                node.ifNull = ifN;
            }
        }

        if (isVoid) return;

        ArrayList<ParamNode> paramNodes;
        ParamNode paramNode;
        boolean contained = node.types.containsKey(parameterType);
        boolean add = true;
        o:
        {
            if (contained) {
                paramNodes = node.types.get(parameterType);
                // we can erase IsNullable
                if (parameterAnnotations.length == 0 || (parameter.isPresent() && parameterAnnotations.length == 1)) {
                    Optional<ParamNode> emptyChoice = paramNodes.stream().filter(p -> p.settings.length == 0).findFirst();
                    if (emptyChoice.isPresent()) {
                        paramNode = emptyChoice.get();
                        add = false;
                        break o;
                    }
                }
            } else {
                paramNodes = new ArrayList<>();
            }
            paramNode = new ParamNode();
            paramNode.node = new Node();
            paramNode.settings = parameterAnnotations;
        }
        treeify(handle, paramTypes, paramAnnotations, returnType, returnAnnotations, variableArity, paramNode.node, arg + 1);
        if (add) {
            paramNodes.add(paramNode);
            if (!contained) {
                node.types.put(parameterType, paramNodes);
            }
        }
    }

    public sealed interface BytecodeCallerApplier permits MethodHandleExecutor, CrackedElementExecutor {
        void apply(MethodVisitor visitor);
    }

    non-sealed static class MethodHandleExecutor implements BytecodeCallerApplier {
        private final int idx;
        MethodType type;

        MethodHandleExecutor(MethodType type, int idx) {
            this.type = type;
            this.idx = idx;
        }

        @Override
        public void apply(MethodVisitor visitor) {
            Class<?>[] parameterArray = type.parameterArray();
            for (int i = parameterArray.length - 1; i >= 0; i--) {
                Class<?> type = parameterArray[i];
                visitor.visitVarInsn(Type.getType(type).getOpcode(ISTORE), i + 8);
            }
            // -> this
            visitor.visitVarInsn(ALOAD, 0);
            // this -> MethodHandle[]
            visitor.visitFieldInsn(
                    GETFIELD,
                    Consts.SURROGATE_CLASS_NAME.getInternalName(),
                    "handles",
                    Type.getDescriptor(MethodHandle[].class)
            );
            // MethodHandle[] -> MethodHandle[] I
            visitor.visitLdcInsn(idx);
            // MethodHandle[] I -> MethodHandle
            visitor.visitInsn(AALOAD);
            // MethodHandle -> thing
            for (int i = 0; i < parameterArray.length; i++) {
                Class<?> type = parameterArray[i];
                visitor.visitVarInsn(Type.getType(type).getOpcode(ILOAD), i + 8);
            }
            visitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    Type.getInternalName(MethodHandle.class),
                    "invokeExact",
                    Type.getMethodDescriptor(
                            Type.getType(type.returnType()),
                            Arrays.stream(type.parameterArray()).map(Type::getType).toArray(Type[]::new)
                    ),
                    false
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, idx);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof MethodHandleExecutor that)) return false;
            return idx == that.idx && Objects.equals(type, that.type);
        }
    }

    non-sealed static class CrackedElementExecutor implements BytecodeCallerApplier {
        private final Object hook;
        Consumer<MethodVisitor> applier;

        CrackedElementExecutor(Object hook, Consumer<MethodVisitor> visitor) {
            this.hook = hook;
            applier = visitor;
        }

        @Override
        public void apply(MethodVisitor visitor) {
            applier.accept(visitor);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(hook);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof CrackedElementExecutor that)) return false;
            return Objects.equals(hook, that.hook);
        }
    }

    public record Executor(BytecodeCallerApplier executor, Class<?> returnType, Annotation[] returnAnnotations) {
    }

    static public class Node {
        public Node ifNull = null;
        public HashMap<Class<?>, ArrayList<ParamNode>> types = new HashMap<>();
        public Map<Pair<Class<?>, Annotation[]>, Executor> executorsByVarargs = new HashMap<>();
    }

    static public class ParamNode {
        public Node node;
        public Annotation[] settings;
    }

    private record HashCodeableOverload(MethodType type, BytecodeCallerApplier applier,
                                        ImmutableList<ImmutableList<Annotation>> parameters,
                                        ImmutableList<Annotation> returnType, boolean variableArity) {
    }

    public static class ASMDispatchGeneratorBuilder {
        ClassMap<CTMethodArgumentConverterHandler> argumentConverters = new ClassMap<>();
        ClassMap<CTMethodArgumentResolverHandler> argumentResolvers = new ClassMap<>();

        public ASMDispatchGeneratorBuilder addArgumentPlugin(CTMethodArgumentConverterHandler type) {
            for (Class<?> handled : type.getHandlingTypes()) {
                argumentConverters.add(handled, type);
            }
            return this;
        }

        public ASMDispatchGeneratorBuilder addArgumentPlugin(CTMethodArgumentResolverHandler type) {
            for (Class<?> handled : type.getHandlingTypes()) {
                argumentResolvers.add(handled, type);
            }
            return this;
        }

        public ASMDispatchGenerator build() {
            return new ASMDispatchGenerator(argumentConverters, argumentResolvers);
        }
    }
}
