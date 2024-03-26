package com.github.applejuiceyy.figuraextras.tech.trees.interfaces;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public interface ObjectDescriber<IN, VALUE> {
    void populateHeader(
            Grid root, Observers.Observer<VALUE> updater,
            Observers.Observer<Optional<VALUE>> freeRoamUpdater,
            ViewChanger objectViewChanger, PopperConsumer popper,
            CyclicReferenceConsumer referenceConsumer,
            Event<Runnable>.Source remover,
            Event<Runnable>.Source ticker);

    Class<IN> getObjectClass();

    interface CyclicReferenceConsumer {
        <O> Observers.Observer<Component> accept(Observers.Observer<Optional<O>> component);
    }

    interface PopperConsumer {
        void accept(Element element, Observers.Observer<Component> value);
    }

    interface ViewChanger {
        <O> void accept(Observers.Observer<Optional<O>> value, Object identity);
    }
}
