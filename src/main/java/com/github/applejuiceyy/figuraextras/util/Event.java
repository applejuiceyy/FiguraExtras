package com.github.applejuiceyy.figuraextras.util;

import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Event<T> {
    Function<List<T>, T> dispatcher;

    ArrayList<T> subscribers = new ArrayList<>();

    ArrayList<Runnable> toRun = new ArrayList<>();
    boolean isFiring = false;

    public Event(Function<List<T>, T> dispatcher) {
        this.dispatcher = dispatcher;
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
        return new Source();
    }

    public Event<T>.Sink getSink() {
        return new Sink();
    }

    public static <T, U, V> Event<TriConsumer<T, U, V>> triConsumer() {
        return new Event<>(v -> (p, l, k) -> v.forEach(n -> n.accept(p, l, k)));
    }

    public static <T, U> Event<BiConsumer<T, U>> biConsumer() {
        return new Event<>(v -> (p, l) -> v.forEach(n -> n.accept(p, l)));
    }

    public static <T> Event<Consumer<T>> consumer() {
        return new Event<>(v -> p -> v.forEach(n -> n.accept(p)));
    }

    public static Event<Runnable> runnable() {
        return new Event<>(v -> () -> v.forEach(Runnable::run));
    }

    public class Source {
        public Runnable subscribe(T subscriber) {
            doWhenAppropriate(() -> subscribers.add(subscriber));
            return () -> subscribers.remove(subscriber);
        }

        public void unsubscribe(T subscriber) {
            doWhenAppropriate(() -> subscribers.remove(subscriber));
        }
    }

    public class Sink {
        public void run(Consumer<T> running) {
            isFiring = true;
            running.accept(dispatcher.apply(subscribers));
            isFiring = false;
            toRun.forEach(Runnable::run);
            toRun.clear();
        }
    }
}
