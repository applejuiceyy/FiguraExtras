package com.github.applejuiceyy.figuraextras.tech.trees.interfaces;

import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.util.Tuple;

import java.util.Optional;

public interface ObjectExpander<IN, KEY, OUT> extends ObjectDescriber<IN, Tuple<KEY, OUT>> {
    // primary generation
    void fetchAllEntries(
            Observers.Observer<Optional<IN>> value,
            Adder adder,
            AddEntry<KEY, OUT> entry,
            Event<Runnable>.Source ticker,
            Event<Runnable>.Source stopUpdatingEntries);


    interface Adder {
        <O> void add(Observers.Observer<Optional<O>> value);
    }

    interface AddEntry<KEY, OUT> {
        void add(Observers.WritableObserver<Optional<Tuple<KEY, OUT>>> value);
    }
}
