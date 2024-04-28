package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.captures.CaptureState;
import org.luaj.vm2.LuaTable;

public interface GlobalsAccess {
    LuaTable figuraExtrass$getOffTheShelfDebugLib();

    void figuraExtrass$setOffTheShelfDebugLib(LuaTable table);

    CaptureState figuraExtrass$getCaptureState();
}
