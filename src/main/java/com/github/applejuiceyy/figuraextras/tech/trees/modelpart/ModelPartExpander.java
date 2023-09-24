package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.util.Tuple;
import org.figuramc.figura.model.FiguraModelPart;

import java.util.ArrayList;
import java.util.Optional;

public class ModelPartExpander implements ObjectExpander<FiguraModelPart, String, FiguraModelPart> {
    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<Tuple<String, FiguraModelPart>> updater, Observers.Observer<Optional<Tuple<String, FiguraModelPart>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {

    }

    @Override
    public Class<FiguraModelPart> getObjectClass() {
        return FiguraModelPart.class;
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<FiguraModelPart>> observer, Adder adder, AddEntry<String, FiguraModelPart> entry, Event<Runnable>.Source ticker, Event<Runnable>.Source stopUpdatingEntries) {
        ArrayList<FiguraModelPart> thing = new ArrayList<>();

        Runnable runnable = ticker.subscribe(() -> {
            Optional<FiguraModelPart> optional = observer.get();
            if (optional.isEmpty()) {
                thing.clear();
                return;
            }
            FiguraModelPart value = optional.get();

            for (FiguraModelPart child : value.children) {
                if (!thing.contains(child)) {
                    thing.add(child);

                    Observers.WritableObserver<Optional<Tuple<String, FiguraModelPart>>> output =
                            Observers.of(Optional.of(new Tuple<>(child.name, child)),
                                    observer.path + "." + child.name
                            );

                    Runnable run = () -> {
                        if (observer.get().isEmpty()) {
                            output.set(Optional.empty());
                            return;
                        }

                        if (!observer.get().get().children.contains(child)) {
                            output.set(Optional.empty());
                        }
                    };

                    Util.subscribeIfNeeded(output, ticker, run);
                    Util.pipeObservation(output, observer);

                    entry.add(output);
                }
            }

            thing.removeIf(e -> !value.children.contains(e));
        });

        stopUpdatingEntries.subscribe(runnable);
    }
}