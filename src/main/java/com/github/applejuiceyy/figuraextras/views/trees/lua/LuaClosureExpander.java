package com.github.applejuiceyy.figuraextras.views.trees.lua;

import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.figuraextras.views.trees.interfaces.ObjectExpander;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.UpValue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class LuaClosureExpander implements ObjectExpander<LuaClosure, Integer, LuaValue> {
    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<Tuple<Integer, LuaValue>> updater, Observers.Observer<Optional<Tuple<Integer, LuaValue>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
        LabelComponent label = Components.label(Component.empty());
        updater.observe(value -> {
            label.text(Component.literal("Upvalue " + value.getA() + ": "));
        });
        root.child(label);
    }

    @Override
    public Class<LuaClosure> getObjectClass() {
        return LuaClosure.class;
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<LuaClosure>> value, Adder adder, AddEntry<Integer, LuaValue> entry, Event<Runnable>.Source ticker, Event<Runnable>.Source stopUpdatingEntries) {
        AtomicInteger currentUpValues = new AtomicInteger(0);

        Runnable subscription = ticker.subscribe(() -> {
            Optional<LuaClosure> currentValue = value.get();

            if (currentValue.isEmpty()) {
                currentUpValues.set(0);
                return;
            }

            LuaClosure currentClosure = currentValue.get();

            for (int i = currentUpValues.get(); i < currentClosure.upValues.length; i++) {
                UpValue v = currentClosure.upValues[i];


                Observers.WritableObserver<Optional<Tuple<Integer, LuaValue>>> observer = Observers.of(Optional.of(new Tuple<>(i, v.getValue())));

                int finalI = i;
                Runnable runnable = () -> observer.set(
                        value.get()
                                .filter(closure -> closure.upValues.length > finalI)
                                .map(closure -> new Tuple<>(finalI, closure.upValues[finalI].getValue()))
                );

                Util.subscribeIfNeeded(observer, ticker, runnable);
                Util.pipeObservation(observer, value);

                entry.add(observer);
            }

            currentUpValues.set(currentClosure.upValues.length);
        });

        stopUpdatingEntries.subscribe(subscription);
    }
}
