package com.github.applejuiceyy.figuraextras.tech.trees.interfaces;

import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public interface ObjectDescriber<IN, VALUE> {
    void populateHeader(
            FlowLayout root, Observers.Observer<VALUE> updater,
            Observers.Observer<Optional<VALUE>> freeRoamUpdater,
            ViewChanger objectViewChanger, PopperConsumer popper,
            CyclicReferenceConsumer referenceConsumer,
            Event<Runnable>.Source remover
    );

    Class<IN> getObjectClass();

    interface CyclicReferenceConsumer {
        <O> Observers.Observer<Component> accept(Observers.Observer<Optional<O>> component);
    }

    interface PopperConsumer {
        void accept(BaseComponent component, Observers.Observer<Component> value);
    }

    interface ViewChanger {
        <O> void accept(Observers.Observer<Optional<O>> value, Object identity);
    }
}
