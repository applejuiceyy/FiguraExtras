package com.github.applejuiceyy.figuraextras.tech.trees.objects;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaWhitelist;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ObjectScraperExpander implements ObjectExpander<Object, String, LuaValue> {
    private static final HashMap<Class<?>, HashMap<String, Function<Object, Object>>> cache = new HashMap<>();

    Avatar avatar;

    public ObjectScraperExpander(Avatar avatar) {
        this.avatar = avatar;
    }

    @Override
    public Class<Object> getObjectClass() {
        return Object.class;
    }

    boolean methodNameIsAppropriate(String name) {
        return name.startsWith("get") || name.startsWith("is");
    }

    HashMap<String, Function<Object, Object>> getAll(Object obj) {
        return getAllGetters(obj.getClass());
    }

    HashMap<String, Function<Object, Object>> getAllGetters(Class<?> cls) {
        if (!cache.containsKey(cls)) {
            HashMap<String, Function<Object, Object>> computed = new HashMap<>();
            fetchGetters(cls, computed);
            cache.put(cls, computed);
        }

        return cache.get(cls);
    }

    private void fetchGetters(Class<?> cls, HashMap<String, Function<Object, Object>> computed) {
        for (Method method : cls.getMethods()) {
            if (method.getDeclaringClass().getAnnotation(LuaWhitelist.class) == null ||
                    method.getAnnotation(LuaWhitelist.class) == null) {
                continue;
            }

            if (!Modifier.isStatic(method.getModifiers()) && method.getParameters().length == 0 && methodNameIsAppropriate(method.getName())) {
                computed.put(":" + method.getName() + "() -> ", v -> {
                    method.setAccessible(true);
                    try {
                        return method.invoke(v);
                    } catch (InvocationTargetException e) {
                        return e.getTargetException();
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        return e;
                    }
                });
            }
        }

        for (Field field : cls.getFields()) {
            if (field.getDeclaringClass().getAnnotation(LuaWhitelist.class) == null ||
                    field.getAnnotation(LuaWhitelist.class) == null) {
                continue;
            }

            if (!Modifier.isStatic(field.getModifiers())) {
                computed.put("." + field.getName() + " -> ", v -> {
                    field.setAccessible(true);
                    try {
                        return field.get(v);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        return e;
                    }
                });
            }
        }
    }


    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<Tuple<String, LuaValue>> updater, Observers.Observer<Optional<Tuple<String, LuaValue>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
        root.child(Components.label(Component.literal(updater.get().getA())));
    }

    public LuaValue wrap(Object object) {
        try {
            return avatar.luaRuntime.typeManager.javaToLua(object).arg1();
        } catch (Exception e) {
            return LuaValue.valueOf(e.getMessage());
        }
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<Object>> value, Adder adder, AddEntry<String, LuaValue> entry, Event<Runnable>.Source ticker, Event<Runnable>.Source stopUpdatingEntries) {
        adder.add(value.derive(in -> {
            if (in.isEmpty()) {
                return Optional.empty();
            }
            if (in.get() instanceof LuaValue) {
                return Optional.empty();
            }
            try {
                return Optional.of(avatar.luaRuntime.typeManager.javaToLua(in));
            } catch (Exception e) {
                return Optional.empty();
            }
        }));


        ArrayList<Function<Object, Object>> observers = new ArrayList<>();

        Observers.UnSubscriber r = value.observe(live -> {
            if (live.isEmpty()) {
                observers.clear();
                return;
            }

            HashMap<String, Function<Object, Object>> o = getAll(live.get());


            for (Map.Entry<String, Function<Object, Object>> stringFunctionEntry : o.entrySet()) {
                if (!observers.contains(stringFunctionEntry.getValue())) {

                    Observers.WritableObserver<Optional<Tuple<String, LuaValue>>> output =
                            Observers.of(Optional.of(new Tuple<>(stringFunctionEntry.getKey(), wrap(stringFunctionEntry.getValue().apply(live.get())))),
                                    value.path + "." + stringFunctionEntry.getKey()
                            );

                    Runnable runnable = () -> {
                        if (value.get().isEmpty()) {
                            output.set(Optional.empty());
                            return;
                        }

                        HashMap<String, Function<Object, Object>> oo = getAll(value.get().get());

                        if (!oo.containsValue(stringFunctionEntry.getValue())) {
                            output.set(Optional.empty());
                            return;
                        }

                        output.set(
                                value.get().map(v -> new Tuple<>(stringFunctionEntry.getKey(), wrap(stringFunctionEntry.getValue().apply(v))))
                        );
                    };

                    Util.subscribeIfNeeded(output, ticker, runnable);
                    Util.pipeObservation(output, value);

                    entry.add(output);

                    observers.add(stringFunctionEntry.getValue());
                }
            }

            observers.removeIf(k -> !o.containsValue(k));
        });

        stopUpdatingEntries.subscribe(r::stop);
    }
}
