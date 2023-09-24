package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.model.FiguraModelPart;

import java.util.Optional;

public class ModelPartInterpreter implements ObjectInterpreter<FiguraModelPart> {
    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<FiguraModelPart> updater, Observers.Observer<Optional<FiguraModelPart>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
        SmallButtonComponent component = new SmallButtonComponent(Component.empty(), 0x00000000);
        root.child(component);
        Object object = new Object();

        component.mouseDown().subscribe((x, y, d) -> {
            objectViewChanger.accept(freeRoamUpdater, object);
            return true;
        });

        remover.subscribe(updater.observe(value -> {
            component.setText(Component.literal(value.name).withStyle(ChatFormatting.BLUE));
        })::stop);
    }

    @Override
    public Class<FiguraModelPart> getObjectClass() {
        return FiguraModelPart.class;
    }
}
