package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.NinePatch;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Rectangle;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class Button extends Grid {
    private Surface normal = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 66 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;
    private Surface disabled = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 46 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;
    private Surface active = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 86 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;

    public Surface getNormal() {
        return normal;
    }

    public Button setNormal(Surface normal) {
        this.normal = normal;
        return this;
    }

    public Surface getDisabled() {
        return disabled;
    }

    public Button setDisabled(Surface disabled) {
        this.disabled = disabled;
        return this;
    }

    public Surface getActive() {
        return active;
    }

    public Button setActive(Surface active) {
        this.active = active;
        return this;
    }


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
        // TODO: differentiate normal surfaces and background surfaces that have access to children and self renders
        // or maybe just make here also compatible
        Rectangle size = getInnerSpace();
        if (activeHovering.get()) {
            active.render(this, graphics, mouseX, mouseY, delta, null, null);
        } else {
            normal.render(this, graphics, mouseX, mouseY, delta, null, null);
        }
    }

    @Override
    protected void defaultActivationBehaviour(DefaultCancellableEvent event) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        getState().setFocused(this);
    }

    @Override
    public boolean blocksMouseActivation() {
        return true;
    }
}
