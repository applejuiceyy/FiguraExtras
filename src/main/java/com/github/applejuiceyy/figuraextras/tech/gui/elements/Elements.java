package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Observers;

import java.util.function.Function;

public final class Elements {
    private Elements() {
    }

    public static Grid margin(Element element, int size) {
        return margin(element, size, size);
    }

    public static Grid margin(Element element, int horizontal, int vertical) {
        return margin(element, horizontal, horizontal, vertical, vertical);
    }

    public static Grid margin(Element element, int left, int right, int top, int bottom) {
        Grid grid = new Grid();
        addMarginsToGrid(grid, left, right, top, bottom);
        grid.add(element).setRow(1).setColumn(1);
        return grid;
    }

    public static void addMarginsToGrid(Grid element, int size) {
        margin(element, size, size);
    }

    public static void addMarginsToGrid(Grid element, int horizontal, int vertical) {
        margin(element, horizontal, horizontal, vertical, vertical);
    }

    public static void addMarginsToGrid(Grid element, int left, int right, int top, int bottom) {
        element.rows()
                .fixed(top)
                .content()
                .fixed(bottom)
                .cols()
                .fixed(left)
                .content()
                .fixed(right);
    }

    public static <T extends ParentElement<?>> T makeVerticalContainerScrollable(T element, Scrollbar verticalScrollbar) {
        return makeVerticalContainerScrollable(element, verticalScrollbar, false);
    }

    public static <T extends ParentElement<?>> T makeVerticalContainerScrollable(T element, Scrollbar verticalScrollbar, boolean hide) {
        element.setClipping(true);
        element.setConstrainY(false);
        TwoWaySetter.track(element.yView, verticalScrollbar.pos, Integer::floatValue, Float::intValue, () -> disableScrollbar(verticalScrollbar, hide));
        element.height.observe(v -> {
            verticalScrollbar.thumbSize = v;
            disableScrollbar(verticalScrollbar, hide);
        });
        element.yViewSize.observe(v -> {
            verticalScrollbar.size = v;
            disableScrollbar(verticalScrollbar, hide);
        });
        element.mouseScrolled.getSource().subscribe(event -> {
            event.cancelPropagation();
            verticalScrollbar.pos.set(Math.min(Math.max(0, (float) (verticalScrollbar.pos.get() - event.amount * 10)), verticalScrollbar.getMax()));
        });
        return element;
    }

    public static <T extends ParentElement<?>> T makeHorizontalContainerScrollable(T element, Scrollbar horizontalScrollbar) {
        return makeHorizontalContainerScrollable(element, horizontalScrollbar, false);
    }

    public static <T extends ParentElement<?>> T makeHorizontalContainerScrollable(T element, Scrollbar horizontalScrollbar, boolean hide) {
        element.setClipping(true);
        element.setConstrainX(false);
        TwoWaySetter.track(element.xView, horizontalScrollbar.pos, Integer::floatValue, Float::intValue, () -> disableScrollbar(horizontalScrollbar, hide));
        element.width.observe(v -> {
            horizontalScrollbar.thumbSize = v;
            disableScrollbar(horizontalScrollbar, hide);
        });
        element.xViewSize.observe(v -> {
            horizontalScrollbar.size = v;
            disableScrollbar(horizontalScrollbar, hide);
        });
        return element;
    }

    private static void disableScrollbar(Scrollbar scroll, boolean hide) {
        boolean enable = scroll.size > scroll.thumbSize && !(scroll.size == 0 || scroll.thumbSize == 0);
        if (hide) {
            ParentElement<?> parent = scroll.getParent();
            if (parent != null) {
                ParentElement.Settings settings = parent.getSettings(scroll);
                if (settings.isInvisible() == enable) {
                    settings.setInvisible(!enable);
                    settings.setDoLayout(enable);
                }
            }
        } else {
            scroll.enabled = enable;
        }
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
