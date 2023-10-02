package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import org.luaj.vm2.LuaTable;

public interface GlobalsAccess {
    ActiveOpportunity<?> figuraExtrass$getCurrentlySearchingForCapture();

    void figuraExtrass$setCurrentlySearchingForCapture(ActiveOpportunity<?> captureOpportunity);

    SecondaryCallHook figuraExtrass$getCurrentCapture();

    void figuraExtrass$setCurrentCapture(SecondaryCallHook hook);

    LuaTable figuraExtrass$getOffTheShelfDebugLib();

    void figuraExtrass$setOffTheShelfDebugLib(LuaTable table);
}
