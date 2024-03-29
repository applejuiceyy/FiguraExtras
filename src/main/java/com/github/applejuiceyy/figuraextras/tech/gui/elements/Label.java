package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.SetText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Comparator;

public class Label extends Element implements SetText {

    private Component text;

    public Label() {
        this(Component.empty());
    }

    public Label(Component component) {
        text = component;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.drawWordWrap(Minecraft.getInstance().font, text, getX(), getY(), getWidth(), 0xffffff);
    }

    @Override
    protected boolean renders() {
        return true;
    }

    Component getText() {
        return text;
    }

    public Label setText(Component text) {
        if (componentsEqual(this.text, text)) {
            this.text = text;
            return this;
        }
        this.text = text;
        enqueueDirtySection(true, false);
        optimalSizeChanged();
        return this;
    }

    // from MutableComponent, mildly modified
    public boolean componentsEqual(Component a, Component b) {
        return a == b || a.getContents().equals(b.getContents()) && a.getStyle().equals(b.getStyle()) && a.getSiblings().equals(b.getSiblings());
    }

    @Override
    public int computeOptimalWidth() {
        return optimalWidthOf(text);
    }

    private int optimalWidthOf(Component text) {
        return Minecraft.getInstance().font
                .split(text, Integer.MAX_VALUE)
                .stream()
                .map(o -> Minecraft.getInstance().font.width(o))
                .max(Comparator.comparingInt(e -> e))
                .orElse(0);
    }

    @Override
    public int computeOptimalHeight(int width) {
        return optimalHeightOf(text, width);
    }

    private int optimalHeightOf(Component text, int width) {
        return Minecraft.getInstance().font.wordWrapHeight(text, width);
    }
}
