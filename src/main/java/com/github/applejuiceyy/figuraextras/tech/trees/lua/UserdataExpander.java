package com.github.applejuiceyy.figuraextras.tech.trees.lua;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.util.Tuple;
import org.luaj.vm2.LuaUserdata;

import java.util.Optional;

public class UserdataExpander implements ObjectExpander<LuaUserdata, Void, Void> {
    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<Tuple<Void, Void>> updater, Observers.Observer<Optional<Tuple<Void, Void>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<LuaUserdata>> value, Adder adder, AddEntry<Void, Void> entry, Event<Runnable>.Source ticker, Event<Runnable>.Source stopUpdatingEntries) {
        adder.add(value.derive(v -> v.map(LuaUserdata::userdata)));
    }

    @Override
    public Class<LuaUserdata> getObjectClass() {
        return LuaUserdata.class;
    }
}
