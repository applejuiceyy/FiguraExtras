package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.util.Observers;
import io.wispforest.owo.ui.core.Component;

import java.util.function.Supplier;

public class Hover {
    static final private Observers.WritableObserver<Object> currentHoverWritable = Observers.of(null);
    static final public Observers.Observer<Object> currentHover = currentHoverWritable;

    public static Runnable setHovering(Object obj) {
        currentHoverWritable.set(obj);
        return () -> {
            if (currentHoverWritable.get() == obj) {
                currentHoverWritable.set(null);
            }
        };
    }

    public static void elementHoverObject(Component component, Supplier<Object> object) {
        Runnable[] canceller = new Runnable[1];

        component.mouseEnter().subscribe(() -> canceller[0] = setHovering(object.get()));

        component.mouseLeave().subscribe(() -> {
            if (canceller[0] == null) return;
            canceller[0].run();
            canceller[0] = null;
        });
    }
}
