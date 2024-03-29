package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.GuiState;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Observers;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Elements {
    private Elements() {
    }

    public static Grid center(Element element) {
        Grid grid = new Grid();
        grid.rows()
                .percentage(1)
                .content()
                .percentage(1)
                .cols()
                .percentage(1)
                .content()
                .percentage(1);
        grid.add(element).setRow(1).setColumn(1);
        return grid;
    }


    public static Grid separator() {
        Grid grid = new Grid();
        grid.rows()
                .fixed(2)
                .fixed(1)
                .fixed(2)
                .cols()
                .percentage(1)
                .percentage(8)
                .percentage(1);
        grid.add(Spacer.nothing().setSurface(Surface.solid(0xff444444))).setRow(1).setColumn(1);
        return grid;
    }

    public static void spawnContextMenu(Element element, BiConsumer<Flow, Runnable> adder) {
        spawnContextMenu(element.getState(), element, adder);
    }

    public static void spawnContextMenu(GuiState state, double mouseX, double mouseY, BiConsumer<Flow, Runnable> adder) {
        spawnContextMenu(state, Observers.of(mouseX), Observers.of(mouseY), adder);
    }

    public static void spawnContextMenu(GuiState state, Element element, BiConsumer<Flow, Runnable> adder) {
        spawnContextMenu(state,
                element.x.derive(Integer::doubleValue),
                element.y.merge(element.height).derive(t -> (double) t.getA() + t.getB()),
                adder
        );
    }

    public static void spawnContextMenu(GuiState state, Observers.Observer<Double> x, Observers.Observer<Double> y, BiConsumer<Flow, Runnable> adder) {
        Flow flow = new Flow();
        Grid wrapping = margin(flow, 2);
        wrapping.setSurface(Surface.contextBackground());
        // TODO: a better way?
        ParentElement<?> host = ((ParentElement<?>) state.getRoot());
        ParentElement.Settings settings = host.add(wrapping).setDoLayout(false).setPriority(100);

        Observers.UnSubscriber xUnsubscriber = x.observe(val -> {
            settings.setX(val.intValue());
        });
        Observers.UnSubscriber yUnsubscriber = y.observe(val -> {
            settings.setY(val.intValue());
        });

        Runnable[] subscribe = new Runnable[1];

        Runnable remover = () -> {
            host.remove(wrapping);
            xUnsubscriber.stop();
            yUnsubscriber.stop();
            subscribe[0].run();
        };

        subscribe[0] = state.mouseDown.subscribe(event -> {
            if (!event.getPropagationPath().contains(wrapping)) {
                remover.run();
            }
        });
        adder.accept(flow, remover);
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
        bindScrollbar(element.height, element.yViewSize, element.yView, verticalScrollbar, hide);
        element.mouseScrolled.getSource().subscribe(event -> {
            event.cancelPropagation();
            verticalScrollbar.pos.set(Math.min(Math.max(0, (float) (verticalScrollbar.pos.get() - event.amount * 10)), verticalScrollbar.getMax()));
        });
        return element;
    }

    private static <T extends ParentElement<?>> void bindScrollbar(Observers.Observer<Integer> sizeObserver, Observers.Observer<Integer> viewSizeObserver, Observers.WritableObserver<Integer> viewPosObserver, Scrollbar verticalScrollbar, boolean hide) {
        int[] ints = new int[3];

        Runnable runnable = () -> {
            if (ints[0] != -1) {
                verticalScrollbar.setThumbSize(ints[0]);
                ints[0] = -1;
            }
            if (ints[1] != -1) {
                verticalScrollbar.setSize(ints[1]);
                ints[1] = -1;
            }
            ints[2] = 0;
            disableScrollbar(verticalScrollbar, hide);
        };

        TwoWaySetter.track(viewPosObserver, verticalScrollbar.pos, Integer::floatValue, Float::intValue, () -> {
            if (ints[2] == 0) verticalScrollbar.getState().childReprocessor.after(runnable);
            ints[2] = 1;
        });
        sizeObserver.observe(v -> {
            if (ints[2] == 0) verticalScrollbar.getState().childReprocessor.after(runnable);
            ints[0] = v;
            ints[2] = 1;
        });
        viewSizeObserver.observe(v -> {
            if (ints[2] == 0) verticalScrollbar.getState().childReprocessor.after(runnable);
            ints[1] = v;
            ints[2] = 1;
        });
    }

    public static <T extends ParentElement<?>> T makeHorizontalContainerScrollable(T element, Scrollbar horizontalScrollbar) {
        return makeHorizontalContainerScrollable(element, horizontalScrollbar, false);
    }

    public static <T extends ParentElement<?>> T makeHorizontalContainerScrollable(T element, Scrollbar horizontalScrollbar, boolean hide) {
        element.setClipping(true);
        element.setConstrainX(false);
        bindScrollbar(element.width, element.xViewSize, element.xView, horizontalScrollbar, hide);
        return element;
    }

    private static void disableScrollbar(Scrollbar scroll, boolean hide) {
        boolean enable = scroll.getSize() > scroll.getThumbSize() && !(scroll.getSize() == 0 || scroll.getThumbSize() == 0);
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

    public static Grid withVerticalScroll(ParentElement<?> content) {
        return withVerticalScroll(content, false);

    }

    public static Grid withVerticalScroll(ParentElement<?> content, boolean hide) {
        Grid ret = new Grid();
        ret.rows()
                .percentage(1)
                .cols()
                .percentage(1)
                .content();
        Scrollbar scrollbar = new Scrollbar();
        ret.add(content);
        ret.add(scrollbar).setColumn(1);
        makeVerticalContainerScrollable(content, scrollbar, hide);
        return ret;
    }

    public static Grid withHorizontalScroll(ParentElement<?> content) {
        return withVerticalScroll(content, false);

    }

    public static Grid withHorizontalScroll(ParentElement<?> content, boolean hide) {
        Grid ret = new Grid();
        ret.rows()
                .percentage(1)
                .content()
                .cols()
                .percentage(1);
        Scrollbar scrollbar = new Scrollbar();
        scrollbar.setHorizontal(true);
        ret.add(content);
        ret.add(scrollbar).setRow(1);
        makeHorizontalContainerScrollable(content, scrollbar, hide);
        return ret;
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
