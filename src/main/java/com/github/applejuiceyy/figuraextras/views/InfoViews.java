package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.screen.MainInfoScreen;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class InfoViews {
    public static ViewConstructor<Context, ConditionalView<Context, Avatar>> ifCondition(Predicate<Avatar> tester, ViewConstructor<Context, ? extends Lifecycle> ifTrue, ViewConstructor<Context, ? extends Lifecycle> ifFalse) {
        return ifCondition(Context::getAvatar, tester, ifTrue, ifFalse);
    }

    public static <V, T> ViewConstructor<V, ConditionalView<V, T>> ifCondition(Function<V, T> toPredicate, Predicate<T> tester, ViewConstructor<V, ? extends Lifecycle> ifTrue, ViewConstructor<V, ? extends Lifecycle> ifFalse) {
        return (thing, additionPoint) -> ifCondition(thing, additionPoint, toPredicate, tester, ifTrue, ifFalse);
    }

    public static <V, T> ConditionalView<V, T> ifCondition(V thing, ParentElement.AdditionPoint additionPoint, Function<V, T> toPredicate, Predicate<T> tester, ViewConstructor<V, ? extends Lifecycle> ifTrue, ViewConstructor<V, ? extends Lifecycle> ifFalse) {
        return new ConditionalView<>(thing, additionPoint, toPredicate, tester, ifTrue, ifFalse);
    }

    public static <V> ConditionalView<V, V> ifCondition(V thing, ParentElement.AdditionPoint additionPoint, Predicate<V> tester, ViewConstructor<V, ? extends Lifecycle> ifTrue, ViewConstructor<V, ? extends Lifecycle> ifFalse) {
        return ifCondition(thing, additionPoint, Function.identity(), tester, ifTrue, ifFalse);
    }

    public static ViewConstructor<Context, ConditionalView<Context, Avatar>> onlyIf(Predicate<Avatar> tester, ViewConstructor<Context, ? extends Lifecycle> ifTrue, Component otherwise) {
        return ifCondition(tester, ifTrue, (avatar, ip) -> new ErrorView(otherwise, ip));
    }

    public static <V, T> ViewConstructor<V, ConditionalView<V, T>> onlyIf(Function<V, T> toPredicate, Predicate<T> tester, ViewConstructor<V, ? extends Lifecycle> ifTrue, Component otherwise) {
        return ifCondition(toPredicate, tester, ifTrue, (avatar, ip) -> new ErrorView(otherwise, ip));
    }

    public static <V, T> ConditionalView<V, T> onlyIf(V thing, ParentElement.AdditionPoint additionPoint, Function<V, T> toPredicate, Predicate<T> tester, ViewConstructor<V, ? extends Lifecycle> ifTrue, Component otherwise) {
        return ifCondition(thing, additionPoint, toPredicate, tester, ifTrue, (avatar, ip) -> new ErrorView(otherwise, ip));
    }

    public static <V> ConditionalView<V, V> onlyIf(V thing, ParentElement.AdditionPoint additionPoint, Predicate<V> tester, ViewConstructor<V, ? extends Lifecycle> ifTrue, Component otherwise) {
        return ifCondition(thing, additionPoint, tester, ifTrue, (avatar, ip) -> new ErrorView(otherwise, ip));
    }

    public static <V> ConditionalView<V, V> onlyIf(V thing, ParentElement.AdditionPoint additionPoint, Predicate<V> tester, ViewConstructor<V, ? extends Lifecycle> ifTrue, String otherwise) {
        return onlyIf(thing, additionPoint, tester, ifTrue, Component.literal(otherwise));
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
