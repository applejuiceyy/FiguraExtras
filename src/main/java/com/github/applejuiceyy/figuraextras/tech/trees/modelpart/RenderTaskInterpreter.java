package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.model.rendertasks.RenderTask;

import java.util.Optional;

public class RenderTaskInterpreter implements ObjectInterpreter<RenderTask> {
    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<RenderTask> updater, Observers.Observer<Optional<RenderTask>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
        SmallButtonComponent component = new SmallButtonComponent(Component.empty(), 0x00000000);
        root.child(component);
        Object object = new Object();

        component.mouseDown().subscribe((x, y, d) -> {
            objectViewChanger.accept(freeRoamUpdater, object);
            return true;
        });

        remover.subscribe(updater.observe(value -> {
            component.setText(Component.literal(value.getName() + " (" + value.getClass().getSimpleName() + ")").withStyle(ChatFormatting.AQUA));
        })::stop);
    }

    @Override
    public Class<RenderTask> getObjectClass() {
        return RenderTask.class;
    }
}
