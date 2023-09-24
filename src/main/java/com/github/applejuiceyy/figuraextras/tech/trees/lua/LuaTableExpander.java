package com.github.applejuiceyy.figuraextras.tech.trees.lua;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.figuramc.figura.avatar.Avatar;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.Optional;

public class LuaTableExpander implements ObjectExpander<LuaTable, LuaValue, LuaValue> {
    private final LuaValueInterpreter interpreter;

    public LuaTableExpander(Avatar avatar) {
        interpreter = new LuaValueInterpreter(avatar);
    }

    @Override
    public Class<LuaTable> getObjectClass() {
        return LuaTable.class;
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<LuaTable>> value, Adder adder,
                                AddEntry<LuaValue, LuaValue> entry,
                                Event<Runnable>.Source ticker,
                                Event<Runnable>.Source stopUpdatingEntries
    ) {
        LuaTable reference = new LuaTable();

        Runnable subscription = ticker.subscribe(() -> updateEntries(value, reference, ticker, entry));

        stopUpdatingEntries.subscribe(subscription);
    }

    public void updateEntries(Observers.Observer<Optional<LuaTable>> observer, LuaTable reference, Event<Runnable>.Source ticker, AddEntry<LuaValue, LuaValue> entry) {
        LuaValue k = LuaValue.NIL;

        Optional<LuaTable> currentValue = observer.get();

        while (currentValue.isPresent()) {
            Varargs n = currentValue.get().next(k);
            if ((k = n.arg1()).isnil())
                break;
            LuaValue v = n.arg(2);

            if (reference.get(k).isnil()) {
                LuaValue finalK1 = k;
                Observers.WritableObserver<Optional<Tuple<LuaValue, LuaValue>>> output =
                        Observers.of(Optional.of(new Tuple<>(k, v)), observer.path + "." + finalK1);
                reference.set(k, v);

                Runnable runnable = () -> {
                    Optional<LuaTable> p = observer.get();

                    output.set(
                            p.filter(m -> !m.get(finalK1).isnil()).map(m -> new Tuple<>(finalK1, m.get(finalK1)))
                    );
                };

                Util.subscribeIfNeeded(output, ticker, runnable);
                Util.pipeObservation(output, observer);

                /*output.shouldListen().subscribe(() -> {
                    FiguraExtras.logger.info("Now listening for " + finalK1 + " inside of table");
                    ticker.subscribe(runnable);
                });
                output.shouldStopListen().subscribe(() -> {
                    FiguraExtras.logger.info("Now no longer listening for " + finalK1 + " inside of table");
                    ticker.unsubscribe(runnable);
                });*/

                entry.add(output);
            }
        }

        while (true) {
            Varargs n = reference.next(k);
            if ((k = n.arg1()).isnil())
                break;

            if (currentValue.isEmpty() || currentValue.get().get(k).isnil()) {
                reference.set(k, LuaValue.NIL);
            }
        }
    }

    private Observers.WritableObserver<Optional<Tuple<LuaValue, LuaValue>>> asObserver(LuaValue k, LuaValue v) {
        return Observers.of(Optional.of(new Tuple<>(k, v)));
    }

    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<Tuple<LuaValue, LuaValue>> updater, Observers.Observer<Optional<Tuple<LuaValue, LuaValue>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
        interpreter.populateHeader(root, updater.derive(Tuple::getA), freeRoamUpdater.derive(v -> v.map(Tuple::getA)), objectViewChanger, popper, referenceConsumer, false);
        root.child(Components.label(Component.literal(": ")));
    }
}
