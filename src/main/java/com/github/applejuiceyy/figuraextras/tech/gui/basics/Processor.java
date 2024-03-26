package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.BiConsumer;

public class Processor<T> {
    ArrayList<T> enqueued = new ArrayList<>();

    ArrayList<Runnable> after = new ArrayList<>();
    BiConsumer<T, Processor<T>> caller;

    Comparator<T> comparator;

    GuiState parent;

    public Processor(BiConsumer<T, Processor<T>> caller, @Nullable Comparator<T> comparator, GuiState parent) {
        this.caller = caller;
        this.comparator = comparator;
        this.parent = parent;
    }

    public void enqueue(T element) {
        if (!enqueued.contains(element)) {
            enqueued.add(element);
        }
    }

    void integrate(Processor<T> processor) {
        for (T t : processor.enqueued) {
            enqueue(t);
        }
        for (Runnable runnable : processor.after) {
            after(runnable);
        }
    }

    public void dequeue(T element) {
        enqueued.remove(element);
    }

    public void after(Runnable runnable) {
        after.add(runnable);
    }

    boolean run() {
        if (comparator != null) {
            enqueued.sort(comparator);
        }

        while (!enqueued.isEmpty()) {
            caller.accept(enqueued.remove(0), this);
        }

        if (after.isEmpty()) return false;
        ArrayList<Runnable> copy = new ArrayList<>(after);
        after.clear();
        copy.forEach(Runnable::run);
        return true;
    }

    void runExhaustively() {
        while (run()) ;
    }
}
