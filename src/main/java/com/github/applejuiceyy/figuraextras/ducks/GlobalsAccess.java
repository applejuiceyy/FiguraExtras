package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.tech.captures.CaptureState;
import org.luaj.vm2.LuaTable;

public interface GlobalsAccess {
    LuaTable figuraExtras$getOffTheShelfDebugLib();

    void figuraExtras$setOffTheShelfDebugLib(LuaTable table);

    CaptureState figuraExtras$getCaptureState();
}
