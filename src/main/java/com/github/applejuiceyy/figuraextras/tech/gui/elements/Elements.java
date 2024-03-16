package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.util.Observers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class Elements {
    private Elements() {
    }

    @Contract("_,null,null->fail")
    public static <T extends ParentElement<?>> T makeContainerScrollable(T element, @Nullable Scrollbar verticalScrollbar, @Nullable Scrollbar horizontalScrollbar) {
        if (verticalScrollbar == null && horizontalScrollbar == null) {
            throw new RuntimeException("both null");
        }
        element.setClipping(true);
        if (verticalScrollbar != null) {
            element.setConstrainY(false);
            TwoWaySetter.track(element.yView, verticalScrollbar.pos, Integer::floatValue, Float::intValue, () -> disableScrollbar(verticalScrollbar));
            element.height.observe(v -> {
                verticalScrollbar.thumbSize = v;
                disableScrollbar(verticalScrollbar);
            });
            element.yViewSize.observe(v -> {
                verticalScrollbar.size = v;
                disableScrollbar(verticalScrollbar);
            });
        }
        if (horizontalScrollbar != null) {
            element.setConstrainX(false);
            TwoWaySetter.track(element.xView, horizontalScrollbar.pos, Integer::floatValue, Float::intValue, () -> disableScrollbar(horizontalScrollbar));
            element.width.observe(v -> {
                horizontalScrollbar.thumbSize = v;
                disableScrollbar(horizontalScrollbar);
            });
            element.xViewSize.observe(v -> {
                horizontalScrollbar.size = v;
                disableScrollbar(horizontalScrollbar);
            });
        }
        element.mouseScrolled.getSource().subscribe(event -> {
            event.cancelPropagation();
            if (verticalScrollbar != null) {
                verticalScrollbar.pos.set(Math.min(Math.max(0, (float) (verticalScrollbar.pos.get() - event.amount * 10)), verticalScrollbar.getMax()));
            }
        });
        return element;
    }

    private static void disableScrollbar(Scrollbar scroll) {
        scroll.enabled = scroll.size > scroll.thumbSize && !(scroll.size == 0 || scroll.thumbSize == 0);
    }

    static class TwoWaySetter<A, B> {
        private final Observers.WritableObserver<A> a;
        private final Observers.WritableObserver<B> b;
        private final Function<A, B> aToBMapper;
        private final Function<B, A> bToAMapper;
        private final Runnable after;
        private boolean working;

        public TwoWaySetter(Observers.WritableObserver<A> a, Observers.WritableObserver<B> b, Function<A, B> aToBMapper, Function<B, A> bToAMapper, Runnable after) {
            working = false;
            a.observe(this::triggerAToB);
            b.observe(this::triggerBToA);
            this.a = a;
            this.b = b;
            this.aToBMapper = aToBMapper;
            this.bToAMapper = bToAMapper;
            this.after = after;
            working = true;
        }

        public static <A, B> TwoWaySetter<A, B> track(Observers.WritableObserver<A> a, Observers.WritableObserver<B> b, Function<A, B> aToBMapper, Function<B, A> bToAMapper, Runnable after) {
            return new TwoWaySetter<>(a, b, aToBMapper, bToAMapper, after);
        }

        public static <A, B> TwoWaySetter<A, B> track(Observers.WritableObserver<A> a, Observers.WritableObserver<B> b, Function<A, B> aToBMapper, Function<B, A> bToAMapper) {
            return new TwoWaySetter<>(a, b, aToBMapper, bToAMapper, () -> {
            });
        }

        public static <T> TwoWaySetter<T, T> track(Observers.WritableObserver<T> a, Observers.WritableObserver<T> b, Runnable after) {
            return new TwoWaySetter<>(a, b, Function.identity(), Function.identity(), after);
        }

        public static <T> TwoWaySetter<T, T> track(Observers.WritableObserver<T> a, Observers.WritableObserver<T> b) {
            return new TwoWaySetter<>(a, b, Function.identity(), Function.identity(), () -> {
            });
        }

        private void triggerAToB(A value) {
            if (!working) return;
            working = false;
            b.set(aToBMapper.apply(value));
            working = true;
        }

        private void triggerBToA(B value) {
            if (!working) return;
            working = false;
            a.set(bToAMapper.apply(value));
            working = true;
        }
    }
}
