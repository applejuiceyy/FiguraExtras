package com.github.applejuiceyy.figuraextras.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.LuaWhitelist;
import org.luaj.vm2.Globals;

@LuaWhitelist
public class DebuggerAPI {
    private final FiguraLuaRuntime runtime;

    public DebuggerAPI(FiguraLuaRuntime runtime) {
        this.runtime = runtime;
    }

    @LuaWhitelist
    public void marker(String name) {
        Globals globals = ((LuaRuntimeAccessor) runtime).getUserGlobals();
        SecondaryCallHook callHook = ((GlobalsAccess) globals).figuraExtrass$getCurrentCapture();
        if (callHook != null) {
            callHook.marker(name);
        }
    }

    @LuaWhitelist
    public void region(String regionName) {
        Globals globals = ((LuaRuntimeAccessor) runtime).getUserGlobals();
        SecondaryCallHook callHook = ((GlobalsAccess) globals).figuraExtrass$getCurrentCapture();
        if (callHook != null) {
            callHook.region(regionName);
        }
    }
}
