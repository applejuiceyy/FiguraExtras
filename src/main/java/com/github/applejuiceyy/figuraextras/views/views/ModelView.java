package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.tech.trees.ObjectTreeView;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.dummy.DummyExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.figuramc.figura.model.FiguraModelPart;

import java.util.ArrayList;
import java.util.Optional;

public class ModelView extends ObjectTreeView<DummyExpander.Dummy> {
    public ModelView(InfoViews.Context context) {
        super(context);
    }

    @Override
    protected Expander<DummyExpander.Dummy> getRootExpander() {
        return ((AvatarAccess) context.getAvatar()).figuraExtrass$getModelViewTree();
    }

    static class ModelPartExpander implements ObjectExpander<FiguraModelPart, String, FiguraModelPart> {
        @Override
        public void populateHeader(FlowLayout root, Observers.Observer<Tuple<String, FiguraModelPart>> updater, Observers.Observer<Optional<Tuple<String, FiguraModelPart>>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
            LabelComponent label = Components.label(Component.empty());
            root.child(label);

            remover.subscribe(updater.observe(value -> {
                label.text(Component.literal(value.getA() + ": ").withStyle(ChatFormatting.BLUE));
            })::stop);
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
}