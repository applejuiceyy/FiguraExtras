package com.github.applejuiceyy.figuraextras.views.views.http;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.views.InfoViews;

public class BodyViewer implements InfoViews.View {

    Flow content = new Flow();
    public BodyViewer(String string) {
        if (string.length() == 0) {
            content.add(net.minecraft.network.chat.Component.literal("No body"));
        } else {
            content.add(net.minecraft.network.chat.Component.literal(string));
        }
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
        return content;
    }
}
