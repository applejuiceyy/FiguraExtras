package com.github.applejuiceyy.figuraextras.views.screen;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.GuiState;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.ViewContainer;
import com.github.applejuiceyy.figuraextras.window.WindowContext;
import com.github.applejuiceyy.figuraextras.window.WindowContextReceiver;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ViewScreen<T> extends Screen implements WindowContextReceiver {
    private final GuiState state;
    private final Button guiScaleButton;
    private final Grid.GridSettings genericMetaHeaderSettings;
    private final ViewContainer container;
    WindowContext context;
    boolean windowIsActive = true;

    Grid genericMetaHeader;

    public ViewScreen(T value, View.ViewConstructor<View.Context<T>, ?> constructor) {
        super(Component.empty());

        Grid root = new Grid();
        root.rows().content().percentage(1).cols().percentage(1);
        state = new GuiState(root);
        state.setUseBackingRenderTarget(!FiguraExtras.disableCachedRendering.value);

        genericMetaHeader = new Grid();
        genericMetaHeaderSettings = root.add(genericMetaHeader);
        genericMetaHeaderSettings.setColumn(0).setRow(0);

        container = new ViewContainer(
                () -> context,
                root.adder(settings -> settings.setRow(1).setColumn(0)),
                t -> genericMetaHeaderSettings.setDoLayout(!t).setInvisible(t)
        );

        genericMetaHeader.setSurface(Surface.solid(0xff111111));
        genericMetaHeader.rows().content().cols().percentage(1).content();

        this.guiScaleButton = (Button) Button.minimal(2).addAnd("Gui Scale: Auto");
        genericMetaHeader.add(guiScaleButton).setColumn(1).setDoLayout(false).setInvisible(true);

        container.setView(constructor, value);
    }

    public void tick() {
        container.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        container.render();
        super.render(graphics, mouseX, mouseY, delta);
    }

    public void dispose() {
        container.dispose();
    }

    @Override
    protected void init() {
        addRenderableWidget(state);
        state.setWidth(width);
        state.setHeight(height);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.getChildAt(mouseX, mouseY).ifPresent(element -> element.mouseMoved(mouseX, mouseY));
    }

    @Override
    public void acknowledge(WindowContext context) {
        this.context = context;
        if (context.canSetGuiScale()) {
            ParentElement.Settings settings = guiScaleButton.getParent().getSettings(guiScaleButton);
            settings.setInvisible(false);
            settings.setDoLayout(true);
            guiScaleButton.activation.subscribe(event -> {
                int current = context.getLockedGuiScale().orElse(0);
                current++;
                if (current >= context.getRecommendedGuiScale()) {
                    context.unlockGuiScale();
                    guiScaleButton.setText(Component.literal("Gui Scale: AUTO"));
                } else {
                    context.lockGuiScale(current);
                    guiScaleButton.setText(Component.literal("Gui Scale: " + current));
                }
            });
        }
    }

    @Override
    public void removed() {
        super.removed();
        container.dispose();
        state.dispose();
    }

    @Override
    public void windowActive(boolean focused) {
        windowIsActive = focused;
    }

    @Override
    public boolean testTransparency(Double x, Double y) {
        return true;
    }
}
