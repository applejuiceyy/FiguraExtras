package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.util.Tuple;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.rendertasks.RenderTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class ModelPartExpander implements ObjectExpander<FiguraModelPart, String, Object> {
    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<Tuple<String, Object>> updater, Observers.Observer<Optional<Tuple<String, Object>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {

    }

    @Override
    public Class<FiguraModelPart> getObjectClass() {
        return FiguraModelPart.class;
    }

    @Override
    public void fetchAllEntries(Observers.Observer<Optional<FiguraModelPart>> observer, Adder adder, AddEntry<String, Object> entry, Event<Runnable>.Source ticker, Event<Runnable>.Source stopUpdatingEntries) {
        ArrayList<FiguraModelPart> modelParts = new ArrayList<>();
        ArrayList<String> renderTasks = new ArrayList<>();

        Runnable runnable = ticker.subscribe(() -> {
            Optional<FiguraModelPart> optional = observer.get();
            if (optional.isEmpty()) {
                modelParts.clear();
                renderTasks.clear();
                return;
            }
            FiguraModelPart value = optional.get();

            for (FiguraModelPart child : value.children) {
                if (!modelParts.contains(child)) {
                    modelParts.add(child);

                    Observers.WritableObserver<Optional<Tuple<String, Object>>> output =
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

            for (Map.Entry<String, RenderTask> renderTask : value.renderTasks.entrySet()) {
                if (!renderTasks.contains(renderTask.getKey())) {
                    renderTasks.add(renderTask.getKey());

                    Observers.WritableObserver<Optional<Tuple<String, Object>>> output =
                            Observers.of(Optional.of(new Tuple<>(renderTask.getKey(), renderTask.getValue())),
                                    observer.path + "." + renderTask.getKey()
                            );

                    Runnable run = () -> {
                        if (observer.get().isEmpty()) {
                            output.set(Optional.empty());
                            return;
                        }

                        if (!observer.get().get().renderTasks.containsKey(renderTask.getKey())) {
                            output.set(Optional.empty());
                        }

                        output.set(Optional.of(new Tuple<>(renderTask.getKey(), observer.get().get().renderTasks.get(renderTask.getKey()))));
                    };

                    Util.subscribeIfNeeded(output, ticker, run);
                    Util.pipeObservation(output, observer);

                    entry.add(output);
                }
            }

            modelParts.removeIf(e -> !value.children.contains(e));
        });

        stopUpdatingEntries.subscribe(runnable);
    }
}