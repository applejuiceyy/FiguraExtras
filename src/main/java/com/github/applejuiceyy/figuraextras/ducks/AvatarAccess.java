package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.views.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.views.trees.dummy.DummyExpander;
import net.minecraft.network.chat.Component;
import org.luaj.vm2.LuaValue;

import java.util.function.BiConsumer;

public interface AvatarAccess {
    boolean figuraExtrass$isCleaned();

    Expander<LuaValue> figuraExtrass$getObjectViewTree();

    Expander<DummyExpander.Dummy> figuraExtrass$getModelViewTree();

    Event<BiConsumer<Component, FiguraLuaPrinterDuck.Kind>> figuraExtrass$getChatRedirect();
}
