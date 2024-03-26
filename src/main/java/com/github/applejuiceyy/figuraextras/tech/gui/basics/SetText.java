package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import net.minecraft.network.chat.Component;

public interface SetText {
    default Element setText(String text) {
        return setText(Component.literal(text));
    }

    Element setText(Component component);
}
