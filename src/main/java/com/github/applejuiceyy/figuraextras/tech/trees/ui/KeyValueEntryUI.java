package com.github.applejuiceyy.figuraextras.tech.trees.ui;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.ContentPopOut;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Entry;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.core.ReferenceStore;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Registration;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.util.Tuple;

import java.util.Optional;
import java.util.function.Consumer;

public class KeyValueEntryUI<K, V> {
    private final Registration registration;
    private final DescriptionUI<Tuple<K, V>> keyDescription;
    private final DescriptionUI<V> valueDescription;
    private final ContentPopOut contentPopOut;
    private final ReferenceStore referenceStore;
    private final Event<Runnable>.Source updater;
    Flow root = new Flow();
    Grid nomenclature = new Grid();
    Grid keyNomenclature = new Grid();
    Grid valueNomenclature = new Grid();
    Flow children = new Flow();
    private EntryUI<?> currentEntry = null;
    private Object expanderIdentity = null;

    public KeyValueEntryUI(Entry<?, K, V> observer, ContentPopOut contentPopOut, ReferenceStore referenceStore, Registration registration, Event<Runnable>.Source updater) {
        this.registration = registration;
        this.contentPopOut = contentPopOut;
        this.referenceStore = referenceStore;
        this.updater = updater;

        nomenclature
                .rows()
                .content()
                .cols()
                .content()
                .content();

        keyNomenclature.rows().percentage(1).cols().percentage(1);
        valueNomenclature.rows().percentage(1).cols().percentage(1);

        root.add(nomenclature);
        root.add(Elements.margin(children, 10, 0, 0, 0));
        nomenclature.add(keyNomenclature);
        nomenclature.add(valueNomenclature).setColumn(1);

        nomenclature.hoveringWithin.observe(
                (Consumer<Boolean>) bool -> nomenclature.setSurface(bool ? Surface.solid(0x11ffffff) : Surface.EMPTY)
        );

        keyDescription = new DescriptionUI<>(
                observer.value().derive(value -> value.map(kvTuple -> new Tuple<>(observer.responsible(), kvTuple))),
                keyNomenclature,
                contentPopOut,
                referenceStore,
                this::switchTo,
                updater
        );

        valueDescription = new DescriptionUI<>(
                observer.value().derive(value -> value.map(kvTuple -> new Tuple<>(findInterpreter(kvTuple.getB()), kvTuple.getB()))),
                valueNomenclature,
                contentPopOut,
                referenceStore,
                this::switchTo,
                updater
        );
    }

    private <O> void switchTo(Observers.Observer<Optional<O>> value, Object identity) {
        if (currentEntry != null) {
            children.remove(currentEntry.childrenLayout);
            currentEntry.dispose();
        }
        if (identity == expanderIdentity) {
            expanderIdentity = null;
            return;
        }
        expanderIdentity = identity;

        currentEntry = new EntryUI<>(
                new Expander<>(value, registration, updater),
                contentPopOut,
                referenceStore,
                registration
        );

        children.add(currentEntry.childrenLayout);
    }

    <O> ObjectInterpreter<O> findInterpreter(O cls) {
        for (ObjectInterpreter<?> interpreter : registration.getInterpreters()) {
            if (interpreter.getObjectClass().isAssignableFrom(cls.getClass())) {
                //noinspection unchecked
                return (ObjectInterpreter<O>) interpreter;
            }
        }
        throw new RuntimeException("Could not find interpreter for class " + cls.getClass().getName());
    }

    public void dispose() {
        if (currentEntry != null) {
            currentEntry.dispose();
        }
        keyDescription.dispose();
        valueDescription.dispose();
    }
}
