package com.github.applejuiceyy.figuraextras.tech.gui.stack;

import org.jetbrains.annotations.Contract;

import java.util.Stack;

abstract public class WorkStack<V> {
    V og;
    Stack<V> stack = new Stack<>();

    public void push() {
        if (stack.isEmpty()) {
            og = outsider();
        }
        V o = stack.isEmpty() ? og : stack.peek();
        stack.push(applyPush(o, declaim(o)));
    }

    public void push(V value) {
        if (stack.isEmpty()) {
            og = outsider();
        }
        stack.push(applyPush(stack.isEmpty() ? og : stack.peek(), value));
    }

    @Contract("true->null;false->!null")
    public V pop(boolean reclaim) {
        V value = stack.pop();
        applyPop(stack.isEmpty() ? og : stack.peek(), value);
        if (reclaim) {
            reclaim(value);
        }
        if (stack.isEmpty()) {
            og = null;
        }
        return reclaim ? null : value;
    }

    public void pop() {
        pop(true);
    }

    public V peek() {
        return stack.isEmpty() ? outsider() : stack.peek();
    }

    protected abstract V declaim(V previous);

    protected abstract V applyPush(V previous, V declaimed);

    protected abstract void applyPop(V newCurrent, V previouslyCurrent);

    public abstract void reclaim(V value);

    protected abstract V outsider();
}
