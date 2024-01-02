package com.github.applejuiceyy.figuraextras.tech.trees.modelpart;

import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.screen.Hover;
import com.github.applejuiceyy.figuraextras.tech.trees.interfaces.ObjectInterpreter;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.model.FiguraModelPart;

import java.util.Optional;

public class ModelPartInterpreter implements ObjectInterpreter<FiguraModelPart> {
    @Override
    public void populateHeader(FlowLayout root, Observers.Observer<FiguraModelPart> updater, Observers.Observer<Optional<FiguraModelPart>> freeRoamUpdater, ViewChanger objectViewChanger, PopperConsumer popper, CyclicReferenceConsumer referenceConsumer, Event<Runnable>.Source remover) {
        SmallButtonComponent component = new SmallButtonComponent(Component.literal(updater.get().name), 0x00000000) {
            @Override
            public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
                FiguraModelPart current = updater.get();

                this.setText(Component.literal(current.name)
                        .withStyle(current.getVisible() ? ChatFormatting.BLUE : ChatFormatting.GRAY)
                );
                super.draw(context, mouseX, mouseY, partialTicks, delta);
            }
        };
        root.child(component);
        Object object = new Object();

        component.mouseDown().subscribe((x, y, d) -> {
            objectViewChanger.accept(freeRoamUpdater, object);
            return true;
        });

        Hover.elementHoverObject(component, updater::get);
    }

    @Override
    public Class<FiguraModelPart> getObjectClass() {
        return FiguraModelPart.class;
    }
}
