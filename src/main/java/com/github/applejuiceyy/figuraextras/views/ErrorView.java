package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import net.minecraft.network.chat.Component;

public class ErrorView implements Lifecycle {
    public ErrorView(Component text, ParentElement.AdditionPoint ip) {
        ip.accept(Elements.center(new Label(text)));
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
