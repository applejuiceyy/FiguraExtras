package com.github.applejuiceyy.figuraextras.tech.trees.dummy;

import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import net.minecraft.util.Tuple;

import java.util.Optional;

public class DummyExpander implements ObjectExpander<DummyExpander.Dummy, Void, Object> {

    @Override
    public void populateHeader(Grid root, Observers.Observer<Tuple<Void, Object>> updater, Observers.Observer<Optional<Tuple<Void, Object>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover, Event<Runnable>.Source ticker) {
    }

    @Override
    public Class<Dummy> getObjectClass() {
        return Dummy.class;
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<Dummy>> observer, Adder adder, AddEntry<Void, Object> entry, Event<Runnable>.Source ticker, Event<Runnable>.Source stopUpdatingEntries) {
        //noinspection unchecked
        Observers.WritableObserver<Optional<Tuple<Void, Object>>>[] output =
                (Observers.WritableObserver<Optional<Tuple<Void, Object>>>[]) new Observers.WritableObserver[]{null};

        observer.observe(value -> {
            if (value.isEmpty()) {
                if (output[0] != null) {
                    output[0].set(Optional.empty());
                    output[0] = null;
                }
            } else if (output[0] == null) {
                output[0] = Observers.of(Optional.of(new Tuple<>(null, value.get().next)), observer.path);
                Util.pipeObservation(output[0], observer);
                entry.add(output[0]);
            } else {
                output[0].set(Optional.of(new Tuple<>(null, value.get().next)));
            }
        });
    }

    public record Dummy(Object next) {
    }
}
