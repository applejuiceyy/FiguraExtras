package com.github.applejuiceyy.figuraextras.util;

import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Event<T> implements SourceLike<T>, SinkLike<T> {
    private final Source source = new Source();
    private final Sink sink = new Sink();
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
        return source;
    }

    public Event<T>.Sink getSink() {
        return sink;
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

    @Override
    public void run(Consumer<T> running) {
        getSink().run(running);
    }

    @Override
    public Runnable subscribe(T subscriber) {
        return getSource().subscribe(subscriber);
    }

    @Override
    public void unsubscribe(T subscriber) {
        getSource().unsubscribe(subscriber);
    }

    public class Source implements SourceLike<T> {
        @Override
        public Runnable subscribe(T subscriber) {
            doWhenAppropriate(() -> subscribers.add(subscriber));
            return () -> doWhenAppropriate(() -> subscribers.remove(subscriber));
        }

        @Override
        public void unsubscribe(T subscriber) {
            doWhenAppropriate(() -> subscribers.remove(subscriber));
        }
    }

    public class Sink implements SinkLike<T> {
        @Override
        public void run(Consumer<T> running) {
            if (subscribers.size() > 0) {
                isFiring = true;
                running.accept(dispatcher.apply(subscribers));
                isFiring = false;
                toRun.forEach(Runnable::run);
                toRun.clear();
            }
        }
    }
}
