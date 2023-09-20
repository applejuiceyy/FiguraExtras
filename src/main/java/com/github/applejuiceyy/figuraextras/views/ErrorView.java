package com.github.applejuiceyy.figuraextras.views;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.network.chat.Component;

public class ErrorView implements InfoViews.View {
    ParentComponent root;

    public ErrorView(Component text) {
        root = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100))
                .child(
                        Components.label(text)

                )
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
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
    public io.wispforest.owo.ui.core.Component getRoot() {
        return root;
    }
}
