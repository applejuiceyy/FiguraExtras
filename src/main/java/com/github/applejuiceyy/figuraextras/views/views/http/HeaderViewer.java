package com.github.applejuiceyy.figuraextras.views.views.http;

import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

public class HeaderViewer implements InfoViews.View {
    FlowLayout content = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
    ScrollContainer<FlowLayout> root = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), content);

    public HeaderViewer(HttpHeaders inputStreamHttpResponse) {
        if (inputStreamHttpResponse.map().size() == 0) {
            content.child(Components.label(net.minecraft.network.chat.Component.literal("No headers")).horizontalSizing(Sizing.fill(100)));
        }
        for (Map.Entry<String, List<String>> stringListEntry : inputStreamHttpResponse.map().entrySet()) {
            for (String s : stringListEntry.getValue()) {
                content.child(Components.label(net.minecraft.network.chat.Component.literal(stringListEntry.getKey() + ": " + s)).horizontalSizing(Sizing.fill(100)));
            }
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
    public Component getRoot() {
        return root;
    }
}
