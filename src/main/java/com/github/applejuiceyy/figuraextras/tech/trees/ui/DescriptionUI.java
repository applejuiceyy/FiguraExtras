package com.github.applejuiceyy.figuraextras.tech.trees.ui;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.ContentPopOut;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.core.ReferenceStore;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectDescriber;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.util.Tuple;

import java.util.Optional;


public class DescriptionUI<V> {
    Grid contentNomenclature = null;

    Event<Runnable> remover = Event.runnable();

    ObjectDescriber<?, V> currentDescriber = null;

    ReferenceStore.ReferenceCreator referenceCreator = null;

    Observers.UnSubscriber sub;


    public DescriptionUI(
            Observers.Observer<Optional<Tuple<ObjectDescriber<?, V>, V>>> observer,
            Grid place,
            ContentPopOut contentPopOut,
            ReferenceStore referenceStore,
            ObjectDescriber.ViewChanger viewChanger, Event<Runnable>.Source updater) {
        Observers.WritableObserver<V> key = Observers.of(observer.get().orElseThrow().getB());

        sub = observer.observe(value -> {
            if (value.isEmpty()) {
                dispose();
                return true;
            }

            if (currentDescriber != value.get().getA()) {
                if (contentNomenclature != null) {
                    place.remove(contentNomenclature);
                }
                if (referenceCreator != null) {
                    referenceCreator.dispose();
                }
                contentNomenclature = new Grid();
                place.add(contentNomenclature);
                remover.getSink().run();
                remover = Event.runnable();

                currentDescriber = value.get().getA();
                key.set(value.get().getB());

                value.get().getA().populateHeader(
                        contentNomenclature,
                        key,
                        observer.derive(v -> v.map(Tuple::getB)),
                        viewChanger,
                        contentPopOut::createPopOut,
                        referenceCreator = referenceStore.referenceCreator(),
                        remover.getSource(),
                        updater
                );
            } else {
                key.set(value.get().getB());
            }
            return false;
        });

    }

    public void dispose() {
        referenceCreator.dispose();
        remover.getSink().run();
        sub.stop();
    }
}
