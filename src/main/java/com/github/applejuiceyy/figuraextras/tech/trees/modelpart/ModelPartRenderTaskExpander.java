package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import net.minecraft.util.Tuple;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.rendertasks.RenderTask;

import java.util.ArrayList;
import java.util.Optional;

public class ModelPartRenderTaskExpander implements ObjectExpander<FiguraModelPart, String, RenderTask> {
    @Override
    public void populateHeader(Grid root, Observers.Observer<Tuple<String, RenderTask>> updater, Observers.Observer<Optional<Tuple<String, RenderTask>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover, Event<Runnable>.Source ticker) {

    }

    @Override
    public Class<FiguraModelPart> getObjectClass() {
        return FiguraModelPart.class;
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<FiguraModelPart>> observer, Adder adder, AddEntry<String, RenderTask> entry, Event<Runnable>.Source ticker, Event<Runnable>.Source stopUpdatingEntries) {
        ArrayList<RenderTask> thing = new ArrayList<>();

        Runnable runnable = ticker.subscribe(() -> {
            Optional<FiguraModelPart> optional = observer.get();
            if (optional.isEmpty()) {
                thing.clear();
                return;
            }
            FiguraModelPart value = optional.get();

            for (RenderTask child : value.renderTasks.values()) {
                if (!thing.contains(child)) {
                    thing.add(child);

                    Observers.WritableObserver<Optional<Tuple<String, RenderTask>>> output =
                            Observers.of(Optional.of(new Tuple<>(child.getName(), child)),
                                    observer.path + "." + child.getName()
                            );

                    Runnable run = () -> {
                        if (observer.get().isEmpty()) {
                            output.set(Optional.empty());
                            return;
                        }

                        if (!observer.get().get().renderTasks.containsValue(child)) {
                            output.set(Optional.empty());
                        }
                    };

                    Util.subscribeIfNeeded(output, ticker, run);
                    Util.pipeObservation(output, observer);

                    entry.add(output);
                }
            }

            thing.removeIf(e -> !value.renderTasks.containsValue(e));
        });

        stopUpdatingEntries.subscribe(runnable);
    }
}