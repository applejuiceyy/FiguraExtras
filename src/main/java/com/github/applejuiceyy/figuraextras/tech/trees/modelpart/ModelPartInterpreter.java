package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.model.FiguraModelPart;

import java.util.Optional;

public class ModelPartInterpreter implements ObjectInterpreter<FiguraModelPart> {
    @Override
    public void populateHeader(Grid root, Observers.Observer<FiguraModelPart> updater, Observers.Observer<Optional<FiguraModelPart>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover, Event<Runnable>.Source ticker) {
        Button component = Button.minimal();
        Label label = new Label();
        component.add(label);

        remover.subscribe(
                ticker.subscribe(() -> {
                    FiguraModelPart part = updater.get();
                    label.setText(Component.literal(part.name)
                            .withStyle(part.getVisible() ? ChatFormatting.BLUE : ChatFormatting.GRAY)
                    );
                })
        );
        root.rows().content().cols().content();
        root.add(component);
        Object object = new Object();

        component.activation.subscribe(event -> objectViewChanger.accept(freeRoamUpdater, object));

        //Hover.elementHoverObject(component, updater::get);
    }

    @Override
    public Class<FiguraModelPart> getObjectClass() {
        return FiguraModelPart.class;
    }
}
