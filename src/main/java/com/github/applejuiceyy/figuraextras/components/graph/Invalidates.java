package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.util.Event;

public class Invalidates {
    private final Event<Runnable> invalidate = Event.runnable();

    Event<Runnable>.Source getInvalidator() {
        return invalidate.getSource();
    }

    protected void invalidate() {
        invalidate.getSink().run();
    }
}
