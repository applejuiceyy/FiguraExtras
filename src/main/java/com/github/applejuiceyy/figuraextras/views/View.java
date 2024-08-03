package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.util.Differential;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.screen.ViewScreen;
import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import com.github.applejuiceyy.figuraextras.window.WindowContext;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.*;

public class View {
    public static <T> void newWindow(T value, ViewConstructor<Context<T>, ?> constructor) {
        FiguraExtras.windows.add(new DetachedWindow(() -> new ViewScreen<>(value, constructor)));
    }

    public static Function<ParentElement.AdditionPoint, ErrorView> error(String err) {
        return ap -> new ErrorView(Component.literal(err), ap);
    }

    public static Function<ParentElement.AdditionPoint, ErrorView> error(Component err) {
        return ap -> new ErrorView(err, ap);
    }

    public static <T, V> ConditionalViewBuilder<T, V> conditional() {
        return new ConditionalViewBuilder<>();
    }

    public static ConditionalViewBuilder<Context<Avatar>, Avatar> context() {
        ConditionalViewBuilder<Context<Avatar>, Avatar> v = new ConditionalViewBuilder<>();
        v.toPredicate(Context::getValue);
        return v;
    }

    public static <T> ConditionalViewBuilder<T, T> simpleConditional() {
        ConditionalViewBuilder<T, T> v = new ConditionalViewBuilder<>();
        v.toPredicate(Function.identity());
        return v;
    }

    public static ConditionalViewBuilder<Void, Void> voiding() {
        return simpleConditional();
    }

    public static <C, I, K> ViewConstructor<C, ? extends Lifecycle> differential(Function<C, Iterable<I>> iterator, Function<I, K> key, ViewConstructor<I, ?> constructor) {
        record Binding(ParentElement.AdditionPoint additionPoint, Lifecycle lifecycle) implements Lifecycle {
            @Override
            public void tick() {
                lifecycle.tick();
            }

            @Override
            public void render() {
                lifecycle.render();
            }

            @Override
            public void dispose() {
                lifecycle.dispose();
                additionPoint.remove();
            }
        }

        return (c, ap) -> new DifferentialView(
                apProvider -> new Differential<>(
                        iterator.apply(c),
                        key,
                        i -> {
                            ParentElement.AdditionPoint additionPoint = apProvider.get();
                            return new Binding(additionPoint, constructor.apply(i, additionPoint));
                        },
                        Lifecycle::dispose
                ),
                ap
        );
    }

    public interface Context<T> {
        T getValue();

        WindowContext getWindowContext();

        <L extends Lifecycle> L setView(View.ViewConstructor<View.Context<T>, L> view);

        <N, L extends Lifecycle> L setView(View.ViewConstructor<View.Context<N>, L> view, N thing);
    }

    public interface ViewConstructor<T, V extends Lifecycle> extends BiFunction<T, ParentElement.AdditionPoint, V> {
    }

    public interface ImplementsMeta {
        void enableMeta();
    }

    public static class ConditionalViewBuilder<T, V> implements ViewConstructor<T, ConditionalView<T, V>> {
        Predicate<V> predicate;
        ViewConstructor<T, ?> ifTrue;
        ViewConstructor<T, ?> ifFalse;

        Function<T, V> toPredicate;

        private ConditionalViewBuilder() {

        }

        public ConditionalViewBuilder<T, V> predicate(Predicate<V> predicate) {
            this.predicate = predicate;
            return this;
        }

        public ConditionalViewBuilder<T, V> predicate(BooleanSupplier predicate) {
            this.predicate = o -> predicate.getAsBoolean();
            return this;
        }

        public ConditionalViewBuilder<T, V> ifTrue(ViewConstructor<T, ?> ifTrue) {
            this.ifTrue = ifTrue;
            return this;
        }

        public ConditionalViewBuilder<T, V> ifFalse(ViewConstructor<T, ?> ifFalse) {
            this.ifFalse = ifFalse;
            return this;
        }

        public ConditionalViewBuilder<T, V> ifTrue(Function<ParentElement.AdditionPoint, ? extends Lifecycle> ifTrue) {
            return ifTrue((p, ap) -> ifTrue.apply(ap));
        }

        public ConditionalViewBuilder<T, V> ifFalse(Function<ParentElement.AdditionPoint, ? extends Lifecycle> ifFalse) {
            return ifFalse((p, ap) -> ifFalse.apply(ap));
        }

        public ConditionalViewBuilder<T, V> ifTrue(Component ifTrue) {
            return ifTrue((c, ip) -> error(ifTrue).apply(ip));
        }

        public ConditionalViewBuilder<T, V> ifFalse(Component ifFalse) {
            return ifFalse((c, ip) -> error(ifFalse).apply(ip));
        }

        public ConditionalViewBuilder<T, V> ifTrue(String ifTrue) {
            return ifTrue(Component.literal(ifTrue));
        }

        public ConditionalViewBuilder<T, V> ifFalse(String ifFalse) {
            return ifFalse(Component.literal(ifFalse));
        }


        public ConditionalViewBuilder<T, V> toPredicate(Function<T, V> toPredicate) {
            this.toPredicate = toPredicate;
            return this;
        }

        public ViewConstructor<T, ConditionalView<T, V>> build() {
            return this;
        }

        public ConditionalView<T, V> build(T thing, ParentElement.AdditionPoint additionPoint) {
            return apply(thing, additionPoint);
        }

        @Override
        public ConditionalView<T, V> apply(T t, ParentElement.AdditionPoint additionPoint) {
            Objects.requireNonNull(toPredicate);
            Objects.requireNonNull(predicate);
            Objects.requireNonNull(ifFalse);
            Objects.requireNonNull(ifTrue);
            return new ConditionalView<>(t, additionPoint, toPredicate, predicate, ifTrue, ifFalse);
        }
    }

    private static class ConditionalView<T, V> implements Lifecycle {
        private final T pass;
        private final Predicate<V> predicate;
        private final ViewConstructor<T, ? extends Lifecycle> ifTrue;
        private final ViewConstructor<T, ? extends Lifecycle> ifFalse;
        private final ParentElement.AdditionPoint additionPoint;

        private final Function<T, V> toPredicate;

        private boolean current;
        private Lifecycle currentView;

        ConditionalView(@Nullable T pass, ParentElement.AdditionPoint additionPoint, Function<T, V> toPredicate, Predicate<V> predicate, ViewConstructor<T, ? extends Lifecycle> ifTrue, ViewConstructor<T, ? extends Lifecycle> ifFalse) {
            this.pass = pass;
            this.toPredicate = toPredicate;
            this.predicate = predicate;
            this.ifTrue = ifTrue;
            this.ifFalse = ifFalse;
            this.additionPoint = additionPoint;
        }

        private void ensureCorrectView() {
            boolean newValue = predicate.test(toPredicate.apply(pass));

            if (currentView == null || newValue != current) {
                if (currentView != null) {
                    currentView.dispose();
                }
                ViewConstructor<T, ? extends Lifecycle> view = newValue ? ifTrue : ifFalse;
                currentView = view.apply(pass, additionPoint);
                current = newValue;
            }
        }

        @Override
        public void tick() {
            ensureCorrectView();
            currentView.tick();
        }

        @Override
        public void render() {
            ensureCorrectView();
            currentView.render();
        }

        @Override
        public void dispose() {
            if (currentView != null) {
                currentView.dispose();
            }
        }
    }

    static class DifferentialView implements Lifecycle {
        Differential<?, ?, ? extends Lifecycle> differential;
        Flow flow;

        DifferentialView(Function<Supplier<ParentElement.AdditionPoint>, Differential<?, ?, ? extends Lifecycle>> differential, ParentElement.AdditionPoint additionPoint) {
            flow = new Flow();
            this.differential = differential.apply(() -> flow.adder(settings -> {
            }));
            additionPoint.accept(Elements.withVerticalScroll(flow));
        }

        @Override
        public void tick() {
            differential.update(Lifecycle::tick);
        }

        @Override
        public void render() {
            differential.update(Lifecycle::render);
        }

        @Override
        public void dispose() {
            differential.dispose();
        }
    }
}
