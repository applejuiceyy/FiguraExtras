package com.github.applejuiceyy.figuraextras.tech.gui.stack;

import org.jetbrains.annotations.Nullable;

public abstract class WorkStackWO<V, O> extends WorkStack<V> {
    public void pushOptions(O options) {
        if (stack.isEmpty()) {
            og = outsider();
        }
        V prev = stack.isEmpty() ? og : stack.peek();
        stack.push(applyPush(prev, declaim(prev, options)));
    }

    @Override
    protected final V declaim(V previous) {
        return declaim(previous, null);
    }

    protected abstract V declaim(V previous, @Nullable O options);
}