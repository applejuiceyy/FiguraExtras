package com.github.applejuiceyy.figuraextras.util;

import net.minecraft.util.Tuple;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.*;

public class Observers {
    private Observers() {
    }

    public static <T> WritableObserver<T> of(T value) {
        return new SimpleObserver<>(value);
    }

    public static <T> WritableObserver<T> of(T value, BiPredicate<T, T> predicate) {
        return new SimpleObserver<>(value, predicate);
    }

    public static <T> WritableObserver<T> of(T value, String path) {
        return new SimpleObserver<>(value, path);
    }

    public static <T> WritableObserver<T> of(T value, BiPredicate<T, T> predicate, String path) {
        return new SimpleObserver<>(value, predicate, path);
    }

    public static final BiPredicate<Object, Object> DEFAULT_COMPARATOR = (v1, v2) -> {
        if (v1 instanceof LuaValue self && v2 instanceof LuaValue other) {
            return self.raweq(other);
        }
        return Objects.equals(v1, v2);
    };

    public static abstract class Observer<VALUE> {
        private final Event<Runnable> startListening = Event.runnable();
        private final Event<Runnable> stopListening = Event.runnable();
        private final ArrayList<Predicate<VALUE>> subscribers = new ArrayList<>();
        private final ArrayList<Runnable> backlog = new ArrayList<>();
        private final int ownId;
        private boolean preventMisfires = false;
        private boolean isFiring = false;

        public abstract VALUE get();

        public final String path;

        private static int id = 0;
        protected static int depth = 0;

        protected Observer(String path) {
            this.path = path;
            this.ownId = id++;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[currentValue=" + get() + "]";
        }

        synchronized private void addListener(Predicate<VALUE> value) {
            if (isFiring) {
                backlog.add(() -> {
                    subscribers.add(value);
                    if (size() == 1) {
                        startListening();
                    }
                });
                return;
            }
            subscribers.add(value);

            if (size() == 1) {
                startListening();
            }
        }

        synchronized private void removeListener(Predicate<VALUE> value) {
            if (isFiring) {
                backlog.add(() -> {
                    if (this.size() > 0) {
                        subscribers.remove(value);
                        if (this.size() == 0) {
                            this.stopListening();
                        }
                    }
                });
                return;
            }

            if (this.size() > 0) {
                subscribers.remove(value);
                if (this.size() == 0) {
                    this.stopListening();
                }
            }
        }

        synchronized private void finishBacklog() {
            backlog.forEach(Runnable::run);
            backlog.clear();
        }

        synchronized protected void fire() {
            if (preventMisfires) return;

            // FiguraExtras.logger.info("    ".repeat(depth) + "Observer " + this.ownId + " preparing to fire");
            depth++;
            if (this.size() > 0) {
                isFiring = true;
                subscribers.removeIf(consumer -> consumer.test(get()));
                isFiring = false;
                if (this.size() == 0) {
                    this.stopListening();
                }
                finishBacklog();
            }

            depth--;
            // FiguraExtras.logger.info("    ".repeat(depth) + "Observer " + this.ownId + " finished firing");
        }

        protected int size() {
            return subscribers.size();
        }

        synchronized public UnSubscriber observe(Predicate<VALUE> value) {
            if (value.test(get())) {
                return new UnSubscriber(this, () -> {
                });
            }
            // FiguraExtras.logger.info("    ".repeat(SimpleObserver.depth) + "Observer " + this.ownId +  " got a listener added");
            addListener(value);

            return new UnSubscriber(this, () -> removeListener(value));
        }

        public UnSubscriber observe(Consumer<VALUE> value) {
            return observe(v -> {
                value.accept(v);
                return false;
            });
        }

        public UnSubscriber observe(Runnable value) {
            return observe(v -> {
                value.run();
                return false;
            });
        }

        public UnSubscriber observe(BooleanSupplier value) {
            return observe(v -> {
                return value.getAsBoolean();
            });
        }

        public ConditionalObservation<VALUE> conditionalObservation(Consumer<VALUE> value) {
            return conditionalObservation(v -> {
                value.accept(v);
                return false;
            });
        }

        public ConditionalObservation<VALUE> conditionalObservation(Predicate<VALUE> value) {
            return new ConditionalObservation<>(this, value);
        }

        public ConditionalObservation<VALUE> conditionalObservation(Runnable value) {
            return conditionalObservation(v -> {
                value.run();
                return false;
            });
        }

        public ConditionalObservation<VALUE> conditionalObservation(BooleanSupplier value) {
            return conditionalObservation(v -> {
                return value.getAsBoolean();
            });
        }

        private void startListening() {
            preventMisfires = true;
            startListening.getSink().run();
            preventMisfires = false;
        }

        private void stopListening() {
            preventMisfires = true;
            stopListening.getSink().run();
            preventMisfires = false;
        }

        public Event<Runnable>.Source shouldListen() {
            return startListening.getSource();
        }

        public Event<Runnable>.Source shouldStopListen() {
            return stopListening.getSource();
        }

        public <OUT> Observer<OUT> derive(Function<VALUE, OUT> transformer) {
            return new DeriveObserver<>(this, transformer, path);
        }

        public Observer<VALUE> proxy(Consumer<UnSubscriber> proxy) {
            return new ProxyObserver<>(this, proxy, path);
        }

        public <OTHER> Observer<Tuple<VALUE, OTHER>> merge(Observer<OTHER> other) {
            return new MergeObserver<>(this, other, path);
        }


        public <OUT> Observer<OUT> derive(Function<VALUE, OUT> transformer, Function<String, String> supplier) {
            return new DeriveObserver<>(this, transformer, supplier.apply(path));
        }

        public Observer<VALUE> proxy(Consumer<UnSubscriber> proxy, Function<String, String> supplier) {
            return new ProxyObserver<>(this, proxy, supplier.apply(path));
        }

        public <OTHER> Observer<Tuple<VALUE, OTHER>> merge(Observer<OTHER> other, Function<String, String> supplier) {
            return new MergeObserver<>(this, other, supplier.apply(path));
        }
    }

    public static class ConditionalObservation<VALUE> {
        private final Observer<VALUE> observer;
        private final Predicate<VALUE> observation;
        private UnSubscriber unsub = null;

        private ConditionalObservation(Observer<VALUE> observer, Predicate<VALUE> observation) {
            this.observer = observer;
            this.observation = observation;
        }

        public void start() {
            if (unsub != null) return;
            unsub = observer.observe(observation);
        }

        public void stop() {
            if (unsub == null) return;
            unsub.stop();
            unsub = null;
        }
    }

    public static class UnSubscriber {
        private final Observer<?> valueObserver;
        private final Runnable unsub;

        private UnSubscriber(Observer<?> observer, Runnable unsub) {
            this.unsub = unsub;
            valueObserver = observer;
        }

        public void stop() {
            // FiguraExtras.logger.info("    ".repeat(SimpleObserver.depth) + "Observer " + valueObserver.ownId +  " got a listener removed");
            unsub.run();
        }
    }

    public abstract static class WritableObserver<VALUE> extends Observer<VALUE> {
        public WritableObserver(String path) {
            super(path);
        }

        public abstract VALUE set(VALUE value);

        public Consumer<VALUE> consumer() {
            return this::set;
        }
    }

    private static class SimpleObserver<VALUE> extends WritableObserver<VALUE> {
        private VALUE value;
        private final BiPredicate<VALUE, VALUE> tester;

        private SimpleObserver(VALUE initial) {
            //noinspection unchecked
            this(initial, (BiPredicate<VALUE, VALUE>) DEFAULT_COMPARATOR);
        }

        private SimpleObserver(VALUE initial, BiPredicate<VALUE, VALUE> tester) {
            this(initial, tester, "");
        }

        public SimpleObserver(VALUE initial, String path) {
            //noinspection unchecked
            this(initial, (BiPredicate<VALUE, VALUE>) DEFAULT_COMPARATOR, path);
        }

        public SimpleObserver(VALUE initial, BiPredicate<VALUE, VALUE> predicate, String path) {
            super(path);
            this.value = initial;
            this.tester = predicate;
        }

        public VALUE get() {
            return value;
        }

        public VALUE set(VALUE value) {
            if (!tester.test(this.value, value)) {
                this.value = value;
                this.fire();
            } else {
                this.value = value;
            }
            return value;
        }
    }

    private static class DeriveObserver<IN, VALUE> extends Observer<VALUE> {

        private final Observer<IN> original;

        private VALUE currentValue;

        private final Function<IN, VALUE> transformer;

        private UnSubscriber unsub = null;

        public DeriveObserver(Observer<IN> original, Function<IN, VALUE> transformer, String path) {
            super(path);
            this.original = original;
            this.transformer = transformer;

            shouldListen().subscribe(() -> {
                unsub = original.observe(incoming -> {
                    VALUE value = transformer.apply(incoming);
                    if (DEFAULT_COMPARATOR.test(currentValue, value)) {
                        currentValue = value;
                    } else {
                        currentValue = value;
                        fire();
                    }
                });
            });

            shouldStopListen().subscribe(() -> {
                unsub.stop();
                unsub = null;
            });
        }

        @Override
        public <OUT> Observer<OUT> derive(Function<VALUE, OUT> transformer, Function<String, String> supplier) {
            return new DeriveObserver<>(original, val -> transformer.apply(this.transformer.apply(val)), supplier.apply(path));
        }

        @Override
        public <OUT> Observer<OUT> derive(Function<VALUE, OUT> transformer) {
            return new DeriveObserver<>(original, val -> transformer.apply(this.transformer.apply(val)), path);
        }

        public VALUE get() {
            if (unsub == null) {
                return transformer.apply(original.get());
            }
            return currentValue;
        }
    }


    private static class ProxyObserver<VALUE> extends Observer<VALUE> {
        private final Observer<VALUE> original;
        private final Consumer<UnSubscriber> consumer;
        private UnSubscriber unsub;

        private ProxyObserver(Observer<VALUE> original, Consumer<UnSubscriber> consumer, String path) {
            super(path);
            this.original = original;
            this.consumer = consumer;

            shouldListen().subscribe(() -> unsub = this.original.observe(value -> {
                this.fire();
            }));

            shouldStopListen().subscribe(() -> unsub.stop());
        }

        public VALUE get() {
            return original.get();
        }

        @Override
        public UnSubscriber observe(Predicate<VALUE> value) {
            UnSubscriber l = super.observe(value);
            consumer.accept(l);
            return l;
        }
    }


    private static class MergeObserver<VALUE, OTHER> extends Observer<Tuple<VALUE, OTHER>> {
        private final Observer<VALUE> self;
        private final Observer<OTHER> other;
        private UnSubscriber unsub;
        private UnSubscriber subsub;

        private MergeObserver(Observer<VALUE> self, Observer<OTHER> other, String path) {
            super(path);
            this.self = self;
            this.other = other;

            shouldListen().subscribe(() -> {
                unsub = self.observe(value -> {
                    fire();
                });
                subsub = other.observe(value -> {
                    fire();
                });
            });

            shouldStopListen().subscribe(() -> {
                unsub.stop();
                subsub.stop();
            });
        }

        public Tuple<VALUE, OTHER> get() {
            return new Tuple<>(self.get(), other.get());
        }
    }
}
