package com.github.applejuiceyy.figuraextras.settings;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.Tuple;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

import static com.github.applejuiceyy.figuraextras.util.Curses.callSuper;

public class SettingsScaffoldBuilder<V> {
    private final static Gson GSON = new Gson();
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final JsonObject container;
    private final Runnable changeListener;
    private final Class<V>[] classes;
    private final Map<String, Tuple<Class<?>, SettingsValue<?>>> values = new HashMap<>();
    private final HashMap<String, List<Tuple<Method, ClassFiller.Overload>>> delayedSetters = new HashMap<>();
    private final ClassFiller<V> filler;
    boolean used = false;
    private boolean conterinerChanged;
    private V _boogeyman;

    @SafeVarargs
    private SettingsScaffoldBuilder(Runnable changeListener, JsonObject container, Class<V>... cls) {
        classes = cls;
        filler = new ClassFiller<>(classes);
        this.container = container;
        this.changeListener = changeListener;
    }

    @SafeVarargs
    public static <P> P build(Runnable changeListener, JsonObject container, Class<P>... cls) {
        return new SettingsScaffoldBuilder<P>(changeListener, container, cls).build();
    }

    @SafeVarargs
    public static <P> P build(File file, Class<P>... cls) {
        JsonObject jsonObject;
        try {
            try {
                InputStream inputStream = new FileInputStream(file);
                byte[] bytes = inputStream.readAllBytes();
                jsonObject = JsonParser.parseString(new String(bytes)).getAsJsonObject();
                inputStream.close();

            } catch (FileNotFoundException | JsonSyntaxException e) {
                jsonObject = new JsonObject();
                Writer out = new FileWriter(file);
                Streams.write(jsonObject, new JsonWriter(out));
                out.close();
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
        JsonObject finalJsonObject = jsonObject;
        return build(() -> {
            try {
                Writer out = new FileWriter(file);
                JsonWriter jsonWriter = new JsonWriter(out);
                jsonWriter.setIndent("    ");
                Streams.write(finalJsonObject, jsonWriter);
                out.close();
            } catch (IOException e) {
                FiguraExtras.logger.error("Failed writing settings", e);
            }
        }, jsonObject, cls);
    }

    V getOrCreateBoogeyman() {
        if (_boogeyman != null) return _boogeyman;
        //noinspection unchecked
        return _boogeyman = (V) Proxy.newProxyInstance(
                SettingsScaffoldBuilder.class.getClassLoader(),
                classes,
                (obj, method, args) -> {
                    if (method.isDefault()) {
                        return callSuper(method, obj, args);
                    }
                    throw new IllegalStateException("Cannot call non-default method");
                }
        );
    }

    V build() {
        try {
            return _build();
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    V _build() throws NoSuchMethodException, IllegalAccessException {
        if (used) throw new IllegalStateException("Already built");
        used = true;


        conterinerChanged = false;


        for (Map.Entry<Method, ClassFiller.Overload> method : filler.methodsToFill()) {
            ClassFiller.Overload overload = method.getValue();
            computeOverload(method.getKey(), overload.type(), overload);
        }

        if (conterinerChanged) {
            changeListener.run();
        }

        return filler.build();
    }

    <T> SettingsValue<T> getOrCreateSettingsValue(String settingName, Class<T> cls) {
        if (values.containsKey(settingName)) {
            Tuple<Class<?>, SettingsValue<?>> tuple = values.get(settingName);

            if (!tuple.getA().equals(cls)) {
                throw new IllegalStateException("Different class: " + tuple.getA() + " and " + cls);
            }

            //noinspection unchecked
            return (SettingsValue<T>) tuple.getB();
        } else {
            SettingsValue<T> settingsValue = new SettingsValue<>() {
                final Event<Consumer<T>> event = Event.consumer();

                @Override
                public void set(T value) {
                    container.add(settingName, GSON.toJsonTree(value, cls));
                    event.getSink().accept(value);
                    changeListener.run();
                }

                @Override
                public T get() {
                    return GSON.fromJson(container.get(settingName), cls);
                }

                @Override
                public Event<Consumer<T>>.Source onChange() {
                    return event.getSource();
                }
            };

            values.put(settingName, new Tuple<>(cls, settingsValue));

            return settingsValue;
        }
    }

    private void computeOverload(Method method, MethodType type, ClassFiller.Overload overload) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle mapped;

        String name = method.getName();
        boolean getter = name.startsWith("get");
        boolean setter = name.startsWith("set");

        Class<?> returnType = type.returnType();

        String settingName;

        if (getter || setter) {
            settingName = name.substring(3, 4).toLowerCase(Locale.ROOT) + name.substring(4);

            if (getter) {
                if (returnType != void.class && type.parameterCount() == 0) {
                    SettingsValue<Object> settingsValue;
                    if (!container.has(settingName)) {
                        if (method.isDefault()) {
                            //noinspection unchecked
                            settingsValue = (SettingsValue<Object>) getOrCreateSettingsValue(settingName, returnType);
                            Object defaultValue;
                            try {
                                method.setAccessible(true);
                                defaultValue = method.invoke(getOrCreateBoogeyman());
                                method.setAccessible(false);
                            } catch (InvocationTargetException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                            settingsValue.set(defaultValue);
                        } else {
                            delayComputation(method, overload, settingName);
                            return;
                        }
                    } else {
                        //noinspection unchecked
                        settingsValue = (SettingsValue<Object>) getOrCreateSettingsValue(settingName, returnType);
                    }

                    mapped = lookup.findVirtual(SettingsValue.class, "get", MethodType.methodType(Object.class));
                    mapped = MethodHandles.insertArguments(mapped, 0, settingsValue);
                    mapped = mapped.asType(mapped.type().changeReturnType(returnType));
                } else {
                    throw new IllegalStateException("getter " + settingName + " has incorrect signature");
                }
            } else {
                if (returnType == void.class && type.parameterCount() == 1) {
                    if (!values.containsKey(settingName)) {
                        delayComputation(method, overload, settingName);
                        return;
                    }
                    Class<?> cls = type.parameterType(0);

                    SettingsValue<?> settingsValue = getOrCreateSettingsValue(settingName, cls);

                    mapped = lookup.findVirtual(SettingsValue.class, "set", MethodType.methodType(void.class, Object.class));
                    mapped = MethodHandles.insertArguments(mapped, 0, settingsValue);
                    mapped = mapped.asType(mapped.type().changeParameterType(0, cls));

                    // prevent running delayed setters and whatnot
                    filler.fillMethod(overload, mapped);
                    return;
                } else {
                    throw new IllegalStateException("setter " + settingName + " has incorrect signature");
                }
            }
        } else if (returnType == SettingsValue.class) {
            Type genericReturnType = method.getGenericReturnType();
            Class<?> cls = null;
            if (genericReturnType instanceof ParameterizedType pt) {
                Type typeArgument = pt.getActualTypeArguments()[0];

                try {
                    cls = Class.forName(typeArgument.getTypeName());
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (cls == null) {
                if (values.containsKey(name)) {
                    FiguraExtras.logger.warn("Could not infer generic type of {}", overload);

                    Tuple<Class<?>, SettingsValue<?>> tuple = values.get(name);
                    cls = tuple.getA();
                } else {
                    delayComputation(method, overload, name);
                    return;
                }
            }

            settingName = name;

            SettingsValue<Object> settingsValue;

            if (!container.has(settingName)) {
                if (method.isDefault()) {
                    //noinspection unchecked
                    settingsValue = (SettingsValue<Object>) getOrCreateSettingsValue(settingName, returnType);
                    SettingsValue<Object> defaultValue;
                    try {
                        method.setAccessible(true);
                        //noinspection unchecked
                        defaultValue = (SettingsValue<Object>) method.invoke(getOrCreateBoogeyman());
                        method.setAccessible(false);
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    settingsValue.set(defaultValue.get());
                } else {
                    delayComputation(method, overload, settingName);
                    return;
                }
            }

            mapped = MethodHandles.constant(SettingsValue.class, getOrCreateSettingsValue(settingName, cls));
        } else {
            if (returnType.isInterface()) {
                if (!container.has(name) || !container.get(name).isJsonObject()) {
                    container.add(name, new JsonObject());
                    conterinerChanged = true;
                }
                mapped = MethodHandles.constant(
                        returnType,
                        new SettingsScaffoldBuilder<>(changeListener, container.getAsJsonObject(name), returnType).build()
                );

                filler.fillMethod(overload, mapped);
                return;
            } else {
                throw new IllegalStateException("Don't know how to wrap " + name + " of type" + returnType);
            }
        }

        filler.fillMethod(overload, mapped);
        runDelayedComputations(settingName);
    }

    private void runDelayedComputations(String settingName) throws NoSuchMethodException, IllegalAccessException {
        if (delayedSetters.containsKey(settingName)) {
            List<Tuple<Method, ClassFiller.Overload>> tuples = delayedSetters.remove(settingName);
            for (Tuple<Method, ClassFiller.Overload> tuple : tuples) {
                computeOverload(tuple.getA(), tuple.getB().type(), tuple.getB());
            }
        }
    }

    private void delayComputation(Method method, ClassFiller.Overload overload, String settingName) {
        delayedSetters
                .computeIfAbsent(settingName, s -> new ArrayList<>())
                .add(new Tuple<>(method, overload));
    }
}
