package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.screen.MainInfoScreen;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

public class InfoViews {
    public static Function<Context, ConditionalView<Context, Avatar>> ifCondition(Predicate<Avatar> tester, Function<Context, ? extends View> ifTrue, Function<Context, ? extends View> ifFalse) {
        return ifCondition(Context::getAvatar, tester, ifTrue, ifFalse);
    }

    public static <V, T> Function<V, ConditionalView<V, T>> ifCondition(Function<V, T> toPredicate, Predicate<T> tester, Function<V, ? extends View> ifTrue, Function<V, ? extends View> ifFalse) {
        return avatar -> ifCondition(avatar, toPredicate, tester, ifTrue, ifFalse);
    }

    public static <V, T> ConditionalView<V, T> ifCondition(V thing, Function<V, T> toPredicate, Predicate<T> tester, Function<V, ? extends View> ifTrue, Function<V, ? extends View> ifFalse) {
        return new ConditionalView<>(thing, toPredicate, tester, ifTrue, ifFalse);
    }

    public static <V> ConditionalView<V, V> ifCondition(V thing, Predicate<V> tester, Function<V, ? extends View> ifTrue, Function<V, ? extends View> ifFalse) {
        return ifCondition(thing, Function.identity(), tester, ifTrue, ifFalse);
    }

    public static Function<Context, ConditionalView<Context, Avatar>> onlyIf(Predicate<Avatar> tester, Function<Context, ? extends View> ifTrue, Component otherwise) {
        return ifCondition(tester, ifTrue, avatar -> new ErrorView(otherwise));
    }

    public static <V, T> Function<V, ConditionalView<V, T>> onlyIf(Function<V, T> toPredicate, Predicate<T> tester, Function<V, ? extends View> ifTrue, Component otherwise) {
        return ifCondition(toPredicate, tester, ifTrue, avatar -> new ErrorView(otherwise));
    }

    public static <V, T> ConditionalView<V, T> onlyIf(V thing, Function<V, T> toPredicate, Predicate<T> tester, Function<V, ? extends View> ifTrue, Component otherwise) {
        return ifCondition(thing, toPredicate, tester, ifTrue, avatar -> new ErrorView(otherwise));
    }

    public static <V> ConditionalView<V, V> onlyIf(V thing, Predicate<V> tester, Function<V, ? extends View> ifTrue, Component otherwise) {
        return ifCondition(thing, tester, ifTrue, avatar -> new ErrorView(otherwise));
    }

    public static <V> ConditionalView<V, V> onlyIf(V thing, Predicate<V> tester, Function<V, ? extends View> ifTrue, String otherwise) {
        return onlyIf(thing, tester, ifTrue, Component.literal(otherwise));
    }

    public interface View extends Lifecycle {
        io.wispforest.owo.ui.core.Component getRoot();
    }

    private static class ConditionalView<T, V> implements View {
        private final T pass;
        private final Predicate<V> predicate;
        private final Function<T, ? extends View> ifTrue;
        private final Function<T, ? extends View> ifFalse;

        private final FlowLayout root = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        private final Function<T, V> toPredicate;

        private boolean current;
        private View currentView;

        ConditionalView(@Nullable T pass, Function<T, V> toPredicate, Predicate<V> predicate, Function<T, ? extends View> ifTrue, Function<T, ? extends View> ifFalse) {
            this.pass = pass;
            this.toPredicate = toPredicate;
            this.predicate = predicate;
            this.ifTrue = ifTrue;
            this.ifFalse = ifFalse;
        }

        private void ensureCorrectView() {
            boolean newValue = predicate.test(toPredicate.apply(pass));

            if (currentView == null || newValue != current) {
                if (currentView != null) {
                    currentView.dispose();
                    currentView.getRoot().remove();
                }
                Function<T, ? extends View> view = newValue ? ifTrue : ifFalse;
                currentView = view.apply(pass);
                root.child(currentView.getRoot());
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

        @Override
        public io.wispforest.owo.ui.core.Component getRoot() {
            return root;
        }
    }

    public interface Context {
        Avatar getAvatar();

        FlowLayout getRoot();

        WindowContentPopOutHost getHost();

        MainInfoScreen getScreen();

        void setView(Function<InfoViews.Context, InfoViews.View> view);
    }
}
