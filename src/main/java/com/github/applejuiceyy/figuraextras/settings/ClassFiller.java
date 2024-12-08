package com.github.applejuiceyy.figuraextras.settings;

import com.github.applejuiceyy.figuraextras.util.Curses;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.github.applejuiceyy.figuraextras.util.Curses.callSuper;

public class ClassFiller<V> {
    private final Class<? super V>[] classes;
    HashMap<Overload, MethodHandle> mapper;
    Map<Method, Overload> methodsToOverloads = new HashMap<>();
    Map<Overload, Method> overloadsToMethods = new HashMap<>();


    @SafeVarargs
    public ClassFiller(Class<? super V>... cls) {
        this.mapper = new HashMap<>();

        Set<Class<? super V>> classes = new HashSet<>();

        for (Class<? super V> cl : cls) {
            walk(cl, c -> {
                classes.add(c);
                for (Method declaredMethod : c.getDeclaredMethods()) {
                    Overload overload;
                    try {
                        overload = new Overload(declaredMethod.getName(), Curses.ALL.unreflect(declaredMethod).type().dropParameterTypes(0, 1));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                    if (!overloadsToMethods.containsKey(overload)) {
                        overloadsToMethods.put(overload, declaredMethod);
                        methodsToOverloads.put(declaredMethod, overload);
                    }
                }
            });
        }

        //noinspection unchecked
        this.classes = classes.toArray(Class[]::new);
    }

    public Class<? super V>[] getFillingClasses() {
        return classes;
    }

    public void fillMethod(Overload overload, MethodHandle mapped) {
        if (mapper.containsKey(overload)) {
            throw new IllegalArgumentException("Method is already filled");
        }
        if (!overload.type.equals(mapped.type())) {
            throw new IllegalArgumentException("Method Handle does not have the correct arguments");
        }

        mapper.put(overload, mapped);
    }

    public boolean wasFilled(Overload overload) {
        return mapper.containsKey(overload);
    }

    public Set<Map.Entry<Method, Overload>> methodsToFill() {
        return methodsToOverloads.entrySet();
    }

    private <P> void walk(Class<? super P> cls, Consumer<Class<? super P>> classConsumer) {
        Class<? super P> superclass = cls.getSuperclass();
        classConsumer.accept(cls);
        if (superclass != null) {
            walk(superclass, classConsumer);
        }
        for (Class<?> anInterface : cls.getInterfaces()) {
            //noinspection unchecked
            walk((Class<? super P>) anInterface, classConsumer);
        }
    }

    public V build() {
        for (Overload overload : overloadsToMethods.keySet()) {
            if (!mapper.containsKey(overload)) {
                throw new IllegalStateException("Missing overload " + overload);
            }
        }
        //noinspection unchecked
        return (V) Proxy.newProxyInstance(
                SettingsScaffoldBuilder.class.getClassLoader(),
                classes,
                (obj, method, args) -> {
                    try {
                        Method original = Object.class.getMethod(method.getName(), method.getParameterTypes());
                        return callSuper(original, obj, args);
                    } catch (Throwable ignored) {
                    }

                    return mapper.get(methodsToOverloads.get(method)).invokeWithArguments(args);
                }
        );
    }

    public record Overload(String name, MethodType type) {
        @Override
        public String toString() {
            return name + type;
        }
    }
}
