package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.model.rendertasks.RenderTask;

import java.util.Optional;

public class RenderTaskInterpreter implements ObjectInterpreter<RenderTask> {
    @Override
    public void populateHeader(Grid root, Observers.Observer<RenderTask> updater, Observers.Observer<Optional<RenderTask>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover, Event<Runnable>.Source ticker) {
        Button component = Button.minimal();
        root.add(component);
        Object object = new Object();

        component.activation.subscribe(event -> objectViewChanger.accept(freeRoamUpdater, object));

        remover.subscribe(updater.observe(value -> {
            component.setText(Component.literal(value.getName() + " (" + value.getClass().getSimpleName() + ")").withStyle(ChatFormatting.AQUA));
        })::stop);
    }

    @Override
    public Class<RenderTask> getObjectClass() {
        return RenderTask.class;
    }
}
