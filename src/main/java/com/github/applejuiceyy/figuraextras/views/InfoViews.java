package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.screen.MainInfoScreen;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

public class InfoViews {
    public static <T, V> ConditionalViewBuilder<T, V> conditional() {
        return new ConditionalViewBuilder<>();
    }

    public static ConditionalViewBuilder<Context, Avatar> context() {
        ConditionalViewBuilder<Context, Avatar> v = new ConditionalViewBuilder<>();
        v.toPredicate(Context::getAvatar);
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

    public interface Context {
        Avatar getAvatar();

        ParentElement<?> getRoot();

        WindowContentPopOutHost getHost();

        MainInfoScreen getScreen();

        void setView(InfoViews.ViewConstructor<InfoViews.Context, Lifecycle> view);
    }

    public interface ViewConstructor<T, V extends Lifecycle> extends BiFunction<T, ParentElement.AdditionPoint, V> {
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
            return ifTrue((c, ip) -> new ErrorView(ifTrue, ip));
        }

        public ConditionalViewBuilder<T, V> ifFalse(Component ifFalse) {
            return ifFalse((c, ip) -> new ErrorView(ifFalse, ip));
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
}
