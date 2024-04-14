package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import com.github.applejuiceyy.figuraextras.util.Event;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;

public interface GlobalsAccess {
    ActiveOpportunity<?> figuraExtrass$getCurrentlySearchingForCapture();

    void figuraExtrass$setCurrentlySearchingForCapture(ActiveOpportunity<?> captureOpportunity);

    @Nullable SecondaryCallHook figuraExtrass$getCurrentCapture();

    Event<SecondaryCallHook>.Source figuraExtrass$getCaptureEventSource();

    LuaTable figuraExtrass$getOffTheShelfDebugLib();

    void figuraExtrass$setOffTheShelfDebugLib(LuaTable table);
}
