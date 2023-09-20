package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.screen.MainInfoScreen;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;

import java.util.function.Function;
import java.util.function.Predicate;

public class InfoViews {
    public interface View {
        void tick();

        io.wispforest.owo.ui.core.Component getRoot();

        void render();

        void dispose();
    }


    public static Function<Context, View> ifCondition(Predicate<Avatar> tester, Function<Context, View> ifTrue, Function<Context, View> ifFalse) {
        return avatar -> new ConditionalView(avatar, tester, ifTrue, ifFalse);
    }

    public static <T> Function<Context, View> onlyIf(Predicate<Avatar> tester, Function<Context, View> ifTrue, Component otherwise) {
        return ifCondition(tester, ifTrue, avatar -> new ErrorView(otherwise));
    }

    private static class ConditionalView implements View {
        private final Context context;
        private final Predicate<Avatar> predicate;
        private final Function<Context, View> ifTrue;
        private final Function<Context, View> ifFalse;

        private final FlowLayout root = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));

        private boolean current;
        private View currentView;

        ConditionalView(Context context, Predicate<Avatar> predicate, Function<Context, View> ifTrue, Function<Context, View> ifFalse) {
            this.context = context;
            this.predicate = predicate;
            this.ifTrue = ifTrue;
            this.ifFalse = ifFalse;
        }

        private void ensureCorrectView() {
            boolean newValue = predicate.test(context.getAvatar());

            if (currentView == null || newValue != current) {
                if (currentView != null) {
                    currentView.dispose();
                    currentView.getRoot().remove();
                }
                Function<Context, View> view = newValue ? ifTrue : ifFalse;
                currentView = view.apply(context);
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
    }
}
