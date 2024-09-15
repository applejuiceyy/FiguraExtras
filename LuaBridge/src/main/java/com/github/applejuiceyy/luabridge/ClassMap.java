package com.github.applejuiceyy.luabridge;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/***
 * Represents a map with classes as keys, but hierarchy rules are obeyed
 * @param <T>
 */
public class ClassMap<T> {
    private final Map<Class<?>, List<T>> o = new HashMap<>();
    private final Map<Class<?>, ArrayList<T>> cache = new HashMap<>();

    public void add(Class<?> type, T o) {
        this.o.computeIfAbsent(type, p -> new ArrayList<>()).add(o);
        cache.clear();
    }

    public void findMapped(Class<?> type, Consumer<T> consumer) {
        if (cache.containsKey(type)) {
            cache.get(type).forEach(consumer);
        }
        collect(type, consumer);
    }

    public List<T> getMapped(Class<?> type) {
        return cache.computeIfAbsent(type, p -> {
            ArrayList<T> objects = new ArrayList<>();
            collect(type, objects::add);
            return objects;
        });
    }

    @Nullable
    public T findFirst(Class<?> type) {
        if (o.containsKey(type)) {
            return o.get(type).get(0);
        }
        Class<?> superclass = type.getSuperclass();
        if (superclass != null) {
            T t = findFirst(superclass);
            if (t != null) {
                return t;
            }
        }
        for (Class<?> anInterface : type.getInterfaces()) {
            T t = findFirst(anInterface);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    @Nullable
    public T findExact(Class<?> type) {
        return o.get(type).isEmpty() ? null : o.get(type).get(0);
    }

    private void collect(Class<?> type, Consumer<T> consumer) {
        if (o.containsKey(type)) {
            o.get(type).forEach(consumer);
        }
        Class<?> superclass = type.getSuperclass();
        if (superclass != null) {
            collect(superclass, consumer);
        }
        for (Class<?> anInterface : type.getInterfaces()) {
            collect(anInterface, consumer);
        }
    }
}
