package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.NinePatch;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.SetText;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class Button extends Grid implements SetText {
    private Surface normal = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 66 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;
    private Surface disabled = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 46 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;
    private Surface active = new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 86 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4);
    ;

    public Button(int left, int right, int top, int bottom, Surface normal, Surface disabled, Surface active) {
        Elements.addMarginsToGrid(this, left, right, top, bottom);
        this.normal = normal;
        this.disabled = disabled;
        this.active = active;
        activeHovering.observe(() -> this.enqueueDirtySection(false, false));
    }

    @Override
    protected boolean renders() {
        return true;
    }

    public static Button minimal() {
        return minimal(0);
    }

    public static Button minimal(int margin) {
        return new Button(margin, margin, margin, margin, Surface.solid(0xff111111), Surface.solid(0x000000), Surface.solid(0xff444444));
    }

    public static Button vanilla() {
        return new Button(10, 10, 5, 5,
                new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 66 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4),
                new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 46 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4),
                new NinePatch(true, new ResourceLocation("textures/gui/widgets.png"), 0, 86 / 256f, 200 / 256f, 20 / 256f, 200, 20, 4, 4, 4, 4)
        );
    }

    public Surface getNormalTexture() {
        return normal;
    }

    public Button setNormalTexture(Surface normal) {
        this.normal = normal;
        return this;
    }

    public Surface getDisabledTexture() {
        return disabled;
    }

    public Button setDisabledTexture(Surface disabled) {
        this.disabled = disabled;
        return this;
    }

    public Surface getActiveTexture() {
        return active;
    }

    public Button setActiveTexture(Surface active) {
        this.active = active;
        return this;
    }

    @Override
    public GridSettings add(Element element) {
        return super.add(element).setRow(1).setColumn(1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // TODO: differentiate normal surfaces and background surfaces that have access to children and self renders
        // or maybe just make here also compatible

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

    @Override
    public Element setText(Component component) {
        for (Element element : getElements()) {
            if (element instanceof Label label) {
                label.setText(component);
                return this;
            }
        }
        add(component);
        return this;
    }
}
