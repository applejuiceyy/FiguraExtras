package com.github.applejuiceyy.figuraextras.util;

import org.apache.logging.log4j.util.TriConsumer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class Event<T> {
    private final Source source = new Source();
    private final T sink;
    Function<List<T>, T> dispatcher;

    ArrayList<T> subscribers = new ArrayList<>();

    ArrayList<Runnable> toRun = new ArrayList<>();

    boolean isFiring = false;

    public Event(BiFunction<List<T>, Firing, T> dispatcher) {
        sink = dispatcher.apply(subscribers, new Firing() {
            @Override
            public void startFiring() {
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
        return interfacing(cls, m -> {
            if (m.getName().equals("toString")) {
                return p -> "Event Proxy";
            }
            throw new RuntimeException("Cannot call method that returns");
        });
    }

    public static <T> Event<T> interfacing(Class<T> cls, Function<Method, Function<Object[], Object>> clashing) {
        //noinspection unchecked
        return new Event<>((v, b) -> (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, (proxy, method, args) -> {
            if (v.isEmpty()) {
                return null;
            }
            b.startFiring();
            method.setAccessible(true);
            try {
                if (v.size() == 1) {
                    if (method.getReturnType() != void.class) {
                        return clashing.apply(method).apply(new Object[]{method.invoke(v.get(0), args)});
                    } else {
                        method.invoke(v.get(0), args);
                        return null;
                    }
                }

                if (method.getReturnType() != void.class) {
                    Function<Object[], Object> solver = clashing.apply(method);
                    List<Object> objects = new ArrayList<>();
                    for (T t : v) {
                        objects.add(method.invoke(t, args));
                    }
                    return solver.apply(objects.toArray());
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
            doWhenAppropriate(() -> subscribers.add(subscriber));
            return () -> doWhenAppropriate(() -> subscribers.remove(subscriber));
        }

        public void unsubscribe(T subscriber) {
            doWhenAppropriate(() -> subscribers.remove(subscriber));
        }
    }
}
