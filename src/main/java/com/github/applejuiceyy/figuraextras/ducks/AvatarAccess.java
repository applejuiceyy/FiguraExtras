package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.dummy.DummyExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import net.minecraft.network.chat.Component;
import org.luaj.vm2.LuaValue;

import java.util.function.BiConsumer;

public interface AvatarAccess {
    boolean figuraExtrass$isCleaned();

    Expander<LuaValue> figuraExtrass$getObjectViewTree();

    Expander<DummyExpander.Dummy> figuraExtrass$getModelViewTree();

    Event<BiConsumer<Component, FiguraLuaPrinterDuck.Kind>> figuraExtrass$getChatRedirect();
}
