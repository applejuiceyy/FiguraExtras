package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.util.SafeCloseable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.BiConsumer;

public class Processor<T> {
    ArrayList<T> enqueued = new ArrayList<>();

    int rejectNew = 0;
    ArrayList<Runnable> after = new ArrayList<>();
    BiConsumer<T, Processor<T>> caller;

    Comparator<T> comparator;

    GuiState parent;

    public Processor(BiConsumer<T, Processor<T>> caller, @Nullable Comparator<T> comparator, GuiState parent) {
        this.caller = caller;
        this.comparator = comparator;
        this.parent = parent;
    }

    public AdditionStatus enqueue(T element) {
        if (!enqueued.contains(element)) {
            if (rejectNew > 0) {
                return AdditionStatus.REJECTED;
            }
            enqueued.add(element);
            return AdditionStatus.OK;
        }
        return AdditionStatus.ALREADY;
    }

    void integrate(Processor<T> processor) {
        for (T t : processor.enqueued) {
            enqueue(t);
        }
        for (Runnable runnable : processor.after) {
            after(runnable);
        }
    }

    public boolean dequeue(T element) {
        return enqueued.remove(element);
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

    public SafeCloseable rejectNewEntries() {
        rejectNew++;
        return () -> rejectNew--;
    }

    void runExhaustively() {
        while (run()) ;
    }

    public enum AdditionStatus {
        OK(true, false), ALREADY(false, false), REJECTED(false, true);

        public final boolean added;
        public final boolean rejected;

        AdditionStatus(boolean added, boolean rejected) {
            this.added = added;
            this.rejected = rejected;
        }
    }
}
