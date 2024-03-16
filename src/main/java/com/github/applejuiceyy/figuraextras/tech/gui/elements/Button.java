package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.NinePatch;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Rectangle;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class Button extends Grid {
    private static final NinePatch normal = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 66 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;
    private static final NinePatch disabled = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 46 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;
    private static final NinePatch active = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 86 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;

    public Button() {
        addRow(6, Grid.SpacingKind.FIXED);
        addRow(0, Grid.SpacingKind.CONTENT);
        addRow(6, Grid.SpacingKind.FIXED);
        addColumn(10, Grid.SpacingKind.FIXED);
        addColumn(0, Grid.SpacingKind.CONTENT);
        addColumn(10, Grid.SpacingKind.FIXED);
    }

    @Override
    public GridSettings add(Element element) {
        return super.add(element).setRow(1).setColumn(1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Rectangle size = getInnerSpace();
        if (activeHovering.get()) {
            active.render(graphics.pose(), size.getX(), size.getY(), size.getWidth(), size.getHeight());
        } else {
            normal.render(graphics.pose(), size.getX(), size.getY(), size.getWidth(), size.getHeight());
        }
    }

    @Override
    protected boolean defaultMouseDownBehaviour(DefaultCancellableEvent.MousePositionButtonEvent event) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        getState().setFocused(this);
        return true;
    }

    @Override
    public boolean hitTest(double mouseX, double mouseY) {
        return true;
    }
}
