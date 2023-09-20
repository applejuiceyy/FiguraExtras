package com.github.applejuiceyy.figuraextras.views.trees.ui;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.ContentPopOut;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.views.trees.core.Entry;
import com.github.applejuiceyy.figuraextras.views.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.views.trees.core.ReferenceStore;
import com.github.applejuiceyy.figuraextras.views.trees.core.Registration;
import com.github.applejuiceyy.figuraextras.views.trees.interfaces.ObjectInterpreter;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.util.Tuple;

import java.util.Optional;

public class KeyValueEntryUI<K, V> {
    private final Registration registration;
    private final DescriptionUI<Tuple<K, V>> keyDescription;
    private final DescriptionUI<V> valueDescription;
    private final ContentPopOut contentPopOut;
    private final ReferenceStore referenceStore;
    private final Event<Runnable>.Source updater;

    private EntryUI<?> currentEntry = null;

    private Object expanderIdentity = null;

    FlowLayout root = Containers.verticalFlow(Sizing.content(), Sizing.content());
    FlowLayout nomenclature = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
    FlowLayout keyNomenclature = Containers.horizontalFlow(Sizing.content(), Sizing.content());
    FlowLayout valueNomenclature = Containers.verticalFlow(Sizing.content(), Sizing.content());
    FlowLayout children = Containers.verticalFlow(Sizing.content(), Sizing.content());

    public KeyValueEntryUI(Entry<?, K, V> observer, ContentPopOut contentPopOut, ReferenceStore referenceStore, Registration registration, Event<Runnable>.Source updater) {
        this.registration = registration;
        this.contentPopOut = contentPopOut;
        this.referenceStore = referenceStore;
        this.updater = updater;

        root.child(nomenclature);
        root.child(children);
        children.padding(Insets.left(20));
        nomenclature.verticalAlignment(VerticalAlignment.CENTER);
        keyNomenclature.verticalAlignment(VerticalAlignment.CENTER);
        valueNomenclature.verticalAlignment(VerticalAlignment.CENTER);
        nomenclature.child(keyNomenclature);
        nomenclature.child(valueNomenclature);
        nomenclature.padding(Insets.of(1));

        nomenclature.mouseEnter().subscribe(() -> nomenclature.surface(Surface.flat(0x11ffffff)));
        nomenclature.mouseLeave().subscribe(() -> nomenclature.surface(Surface.BLANK));

        keyDescription = new DescriptionUI<>(
                observer.value().derive(value -> value.map(kvTuple -> new Tuple<>(observer.responsible(), kvTuple))),
                keyNomenclature,
                contentPopOut,
                referenceStore,
                this::switchTo
        );

        valueDescription = new DescriptionUI<>(
                observer.value().derive(value -> value.map(kvTuple -> new Tuple<>(findInterpreter(kvTuple.getB()), kvTuple.getB()))),
                valueNomenclature,
                contentPopOut,
                referenceStore,
                this::switchTo
        );
    }

    private <O> void switchTo(Observers.Observer<Optional<O>> value, Object identity) {
        if (currentEntry != null) {
            children.removeChild(currentEntry.childrenLayout);
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

        children.child(currentEntry.childrenLayout);
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
