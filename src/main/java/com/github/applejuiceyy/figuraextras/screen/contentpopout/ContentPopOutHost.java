package com.github.applejuiceyy.figuraextras.screen.contentpopout;

import com.github.applejuiceyy.figuraextras.util.Observers;
import net.minecraft.network.chat.Component;

public interface ContentPopOutHost {
    void add(Observers.Observer<Component> component);
}
