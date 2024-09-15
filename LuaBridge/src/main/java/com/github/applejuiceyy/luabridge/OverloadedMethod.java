package com.github.applejuiceyy.luabridge;

import com.google.common.collect.ImmutableList;
import org.luaj.vm2.Varargs;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;


public class OverloadedMethod {
    private final String name;
    private final Bridge bridge;
    private List<Overload> overloads = new ArrayList<>();

    public OverloadedMethod(String name, Bridge bridge) {
        this.name = name;
        this.bridge = bridge;
    }

    public String getName() {
        return name;
    }

    public void addOverload(MethodHandle methodHandle) {
        MethodHandleInfo handleInfo;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            handleInfo = lookup.revealDirect(methodHandle);
            int referenceKind = handleInfo.getReferenceKind();

            // behaviour taken from java.lang.invoke.InfoFromMemberName.reflectUnchecked
            if (referenceKind <= MethodHandleInfo.REF_putStatic) {
                Class<?> returnType = methodHandle.type().returnType();
                boolean maySet = returnType == Void.class;
                addOverload(handleInfo.reflectAs(Field.class, lookup), maySet);
            } else if (referenceKind == MethodHandleInfo.REF_newInvokeSpecial) {
                addOverload(handleInfo.reflectAs(Constructor.class, lookup));
            } else {
                addOverload(handleInfo.reflectAs(Method.class, lookup));
            }
        } catch (IllegalArgumentException | IllegalAccessException arg) {
            Annotation[][] params = new Annotation[methodHandle.type().parameterCount()][];
            Arrays.fill(params, new Annotation[0]);
            addOverload(
                    methodHandle,
                    new Annotation[0],
                    params
            );
        }
    }

    public void addOverload(Field field, boolean write) throws IllegalAccessException {
        MethodHandle handle;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        handle = write ? lookup.unreflectSetter(field) : lookup.unreflectGetter(field);
        Annotation[][] params;
        Annotation[] ret;
        if (write) {
            if (Modifier.isStatic(field.getModifiers())) {
                params = new Annotation[][]{field.getAnnotations()};
            } else {
                params = new Annotation[][]{
                        new Annotation[0],
                        field.getAnnotations()
                };
            }
            ret = new Annotation[0];
        } else {
            if (Modifier.isStatic(field.getModifiers())) {
                params = new Annotation[0][];
            } else {
                params = new Annotation[][]{new Annotation[0]};
            }
            ret = field.getAnnotations();
        }
        addOverload(handle, ret, params);
    }

    public void addOverload(Constructor<?> constructor) throws IllegalAccessException {
        MethodHandle handle;
        handle = MethodHandles.lookup().unreflectConstructor(constructor);
        addOverload(handle, new Annotation[0], readParams(constructor));
    }

    public void addOverload(Method method) throws IllegalAccessException {
        MethodHandle handle;
        handle = MethodHandles.lookup().unreflect(method);
        Annotation[][] params = readParams(method);
        if (!Modifier.isStatic(method.getModifiers())) {
            Annotation[][] wt = new Annotation[params.length + 1][];
            System.arraycopy(params, 0, wt, 1, params.length);
            wt[0] = new Annotation[0];
            params = wt;
        }
        addOverload(handle, method.getAnnotations(), params);
    }

    public void addOverload(MethodHandle handle, Annotation[] returnType, Annotation[][] parameters) {
        overloads.add(new Overload(handle, parameters, returnType));
    }

    private Annotation[][] readParams(Executable executable) {
        java.lang.reflect.Parameter[] params = executable.getParameters();
        Annotation[][] settings = new Annotation[params.length][];
        for (int i = 0, paramsLength = params.length; i < paramsLength; i++) {
            java.lang.reflect.Parameter parameter = params[i];
            settings[i] = parameter.getAnnotations();
        }
        return settings;
    }

    public void addOverload(Field field) throws IllegalAccessException {
        addOverload(field, false);
        addOverload(field, true);
    }

    public <N> void addOverload(N object) {
        addOverload(object, Function.identity());
    }

    public void addOverload(Object obj, Function<MethodHandle, MethodHandle> typeSetter) {
        Class<?> type = obj.getClass();
        Class<?>[] interfaces = type.getInterfaces();
        if (interfaces.length != 1) {
            throw new IllegalArgumentException("Not a functional interface implementation");
        }
        Class<?> i = interfaces[0];
        Method[] array = Arrays.stream(i.getMethods()).filter(o -> !o.isDefault()).toArray(Method[]::new);
        if (array.length != 1) {
            throw new IllegalArgumentException("Not a functional interface");
        }
        Method method = array[0];
        Annotation[][] params = readParams(method);
        MethodHandle handle;
        try {
            handle = MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        MethodHandle bound = handle.bindTo(obj);

        bound = typeSetter.apply(bound);

        addOverload(bound, method.getAnnotations(), params);
    }

    public <N> void addOverload(N object, MethodType type) {
        addOverload(object, handle -> handle.asType(type));
    }

    Function<Varargs, Varargs> generateDispatch() {
        DispatchGenerator.Dispatch dispatch = bridge.getDispatchGenerator().generateDispatch(this, ImmutableList.copyOf(overloads));
        return varargs -> dispatch.dispatch(bridge.getLuaRuntime(), bridge, varargs);
    }

    public record Overload(MethodHandle handle, Annotation[][] argumentAnnotations, Annotation[] returnAnnotation) {

    }
}
