package com.github.applejuiceyy.figuraextras.util;

import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Event<T> {
    private final Source source = new Source();
    private final T sink;

    ArrayList<T> subscribers = new ArrayList<>();

    ArrayList<Runnable> toRun = new ArrayList<>();

    boolean isFiring = false;

    public Event(BiFunction<List<T>, Firing, T> dispatcher) {
        sink = dispatcher.apply(subscribers, new Firing() {
            @Override
            public void startFiring() {
                if (isFiring) {
                    throw new IllegalStateException("Already firing");
                }
                isFiring = true;
            }

            @Override
            public void finishFiring() {
                isFiring = false;
                toRun.forEach(Runnable::run);
                toRun.clear();
            }
        });
    }

    public boolean hasSubscribers() {
        return subscribers.size() > 0;
    }

    private void doWhenAppropriate(Runnable runnable) {
        if (isFiring) {
            toRun.add(runnable);
            return;
        }
        runnable.run();
    }

    public Event<T>.Source getSource() {
        return source;
    }

    public static <T, U, V> Event<TriConsumer<T, U, V>> triConsumer() {
        return new Event<>((v, b) -> (p, l, k) -> {
            b.startFiring();
            v.forEach(n -> n.accept(p, l, k));
            b.finishFiring();
        });
    }

    public static <T, U> Event<BiConsumer<T, U>> biConsumer() {
        return new Event<>((v, b) -> (p, l) -> {
            b.startFiring();
            v.forEach(n -> n.accept(p, l));
            b.finishFiring();
        });
    }

    public static <T> Event<Consumer<T>> consumer() {
        return new Event<>((v, b) -> p -> {
            b.startFiring();
            v.forEach(n -> n.accept(p));
            b.finishFiring();
        });
    }

    public static Event<Runnable> runnable() {
        return new Event<>((v, b) -> () -> {
            b.startFiring();
            v.forEach(Runnable::run);
            b.finishFiring();
        });
    }

    public static <T> Event<T> interfacing(Class<T> cls) {
        return interfacing(cls, (m, obj) -> {
            throw new RuntimeException("Cannot call method that returns");
        });
    }

    public static <T> Event<T> interfacing(Class<T> cls, BiFunction<Method, Object[], Object> clashing) {
        //noinspection unchecked
        return new Event<>((v, b) -> (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, (proxy, method, args) -> {
            if (method.getName().equals("toString")) {
                return "Event Proxy";
            }
            if (v.isEmpty()) {
                return null;
            }
            b.startFiring();
            method.setAccessible(true);
            try {
                if (v.size() == 1) {
                    if (method.getReturnType() != void.class) {
                        return clashing.apply(method, new Object[]{method.invoke(v.get(0), args)});
                    } else {
                        method.invoke(v.get(0), args);
                        return null;
                    }
                }

                if (method.getReturnType() != void.class) {
                    Object[] objects = new Object[v.size()];
                    for (int i = 0; i < v.size(); i++) {
                        T t = v.get(i);
                        objects[i] = method.invoke(t, args);
                    }
                    return clashing.apply(method, objects);
                }
                for (T t : v) {
                    method.invoke(t, args);
                }
                return null;
            } finally {
                method.setAccessible(false);
                b.finishFiring();
            }
        }));
    }

    public T getSink() {
        return sink;
    }

    public @Nullable T getNullableSink() {
        return hasSubscribers() ? sink : null;
    }

    public boolean isActive() {
        return isFiring;
    }

    public Iterable<T> iterateSink() {
        return subscribers;
    }

    public Runnable subscribe(T subscriber) {
        doWhenAppropriate(() -> subscribers.add(subscriber));
        return () -> doWhenAppropriate(() -> subscribers.remove(subscriber));
    }

    public void unsubscribe(T subscriber) {
        doWhenAppropriate(() -> subscribers.remove(subscriber));
    }

    public interface Firing {
        void startFiring();

        void finishFiring();
    }

    public class Source {
        public Runnable subscribe(T subscriber) {
            return Event.this.subscribe(subscriber);
        }

        public void unsubscribe(T subscriber) {
            Event.this.unsubscribe(subscriber);
        }
    }
}
