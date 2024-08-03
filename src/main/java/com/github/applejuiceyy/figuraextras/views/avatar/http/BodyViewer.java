package com.github.applejuiceyy.figuraextras.views.avatar.http;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;

public class BodyViewer implements Lifecycle {

    Flow content = new Flow();

    public BodyViewer(String string, ParentElement.AdditionPoint additionPoint) {
        if (string.length() == 0) {
            content.add(net.minecraft.network.chat.Component.literal("No body"));
        } else {
            content.add(net.minecraft.network.chat.Component.literal(string));
        }
        additionPoint.accept(Elements.withVerticalScroll(content));
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
}
