package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import net.minecraft.network.chat.Component;

public class ErrorView implements InfoViews.View {
    Element root;

    public ErrorView(Component text) {
        root = Elements.center(new Label(text));
    }

    @Override
    public void tick() {
    }

    @Override
    public void render() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Element getRoot() {
        return root;
    }
}
