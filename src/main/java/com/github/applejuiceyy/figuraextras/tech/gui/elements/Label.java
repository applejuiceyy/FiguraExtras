package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.SetText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Comparator;
import java.util.List;

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

    public Style getComponentAt(double x, double y) {
        int i = (int) ((y - this.y.get() - 1) / Minecraft.getInstance().font.lineHeight);
        List<FormattedCharSequence> split = Minecraft.getInstance().font.split(text, width.get());
        if (i >= 0 && i < split.size()) {
            FormattedCharSequence line = split.get(split.size() - 1 - i);
            return Minecraft.getInstance().font.getSplitter().componentStyleAtWidth(line, Mth.floor(x - this.x.get()));
        }
        return null;
    }

    // from MutableComponent, mildly modified
    public boolean componentsEqual(Component a, Component b) {
        return a == b || a.getContents().equals(b.getContents()) && a.getStyle().equals(b.getStyle()) && a.getSiblings().equals(b.getSiblings());
    }


    @Override
    public HoverIntent mouseHoverIntent(double mouseX, double mouseY) {
        Style style = getComponentAt(mouseX, mouseY);
        if (style == null) return HoverIntent.NONE;
        if (style.getClickEvent() != null) return HoverIntent.INTERACT;
        if (style.getHoverEvent() != null) return HoverIntent.LOOK;
        return HoverIntent.NONE;
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
