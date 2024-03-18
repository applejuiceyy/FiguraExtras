package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Comparator;

public class Label extends Element {

    private Component text;

    public Label() {
        this(Component.empty());
    }

    public Label(MutableComponent component) {
        text = component;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.drawWordWrap(Minecraft.getInstance().font, text, getX(), getY(), getWidth(), 0xffffff);
    }

    Component getText() {
        return text;
    }

    public Label setText(Component text) {
        this.text = text;
        markLayoutDirty();
        return this;
    }

    @Override
    public int getOptimalWidth() {
        return Minecraft.getInstance().font
                .split(text, Integer.MAX_VALUE)
                .stream()
                .map(o -> Minecraft.getInstance().font.width(o))
                .max(Comparator.comparingInt(e -> e))
                .orElseThrow();
    }

    @Override
    public int getOptimalHeight(int width) {
        return Minecraft.getInstance().font.wordWrapHeight(text, width);
    }
}
