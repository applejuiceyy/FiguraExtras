package com.github.applejuiceyy.figuraextras.views.views.http;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

public class HeaderViewer implements Lifecycle {
    Flow content = new Flow();

    public HeaderViewer(HttpHeaders inputStreamHttpResponse, ParentElement.AdditionPoint additionPoint) {
        if (inputStreamHttpResponse.map().size() == 0) {
            content.add(net.minecraft.network.chat.Component.literal("No headers"));
        }
        for (Map.Entry<String, List<String>> stringListEntry : inputStreamHttpResponse.map().entrySet()) {
            for (String s : stringListEntry.getValue()) {
                content.add(net.minecraft.network.chat.Component.literal(stringListEntry.getKey() + ": " + s));
            }
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
