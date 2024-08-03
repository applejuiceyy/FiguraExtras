package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.window.WindowContext;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import java.util.function.Supplier;

public class ViewContainer implements Lifecycle {
    private final Supplier<WindowContext> windowContextSupplier;
    private final ParentElement.AdditionPoint additionPoint;
    Lifecycle current;
    BooleanConsumer meta = null;

    public ViewContainer(Supplier<WindowContext> windowContextSupplier, ParentElement.AdditionPoint additionPoint) {
        this.windowContextSupplier = windowContextSupplier;
        this.additionPoint = additionPoint;
    }

    public ViewContainer(Supplier<WindowContext> windowContextSupplier, ParentElement.AdditionPoint additionPoint, BooleanConsumer meta) {
        this.windowContextSupplier = windowContextSupplier;
        this.meta = meta;
        this.additionPoint = additionPoint;
    }

    public <N, L extends Lifecycle> L setView(View.ViewConstructor<View.Context<N>, L> constructor, N value) {
        if (current != null) {
            current.dispose();
        }
        L applied = constructor.apply(new View.Context<N>() {
            @Override
            public N getValue() {
                return value;
            }

            @Override
            public WindowContext getWindowContext() {
                return windowContextSupplier.get();
            }

            @Override
            public <L1 extends Lifecycle> L1 setView(View.ViewConstructor<View.Context<N>, L1> view) {
                return setView(view, value);
            }

            @Override
            public <N1, L1 extends Lifecycle> L1 setView(View.ViewConstructor<View.Context<N1>, L1> view, N1 thing) {
                return ViewContainer.this.setView(view, thing);
            }
        }, additionPoint);
        current = applied;
        if (meta != null) {
            if (current instanceof View.ImplementsMeta implementer) {
                implementer.enableMeta();
                meta.accept(true);
            } else {
                meta.accept(false);
            }
        }
        return applied;
    }

    @Override
    public void tick() {
        if (current != null) {
            current.tick();
        }
    }

    @Override
    public void render() {
        if (current != null) {
            current.render();
        }
    }

    @Override
    public void dispose() {
        if (current != null) {
            current.dispose();
            additionPoint.remove();
        }
    }
}
