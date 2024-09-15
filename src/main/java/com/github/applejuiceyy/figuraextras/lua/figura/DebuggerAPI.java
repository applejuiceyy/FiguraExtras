package com.github.applejuiceyy.figuraextras.lua.figura;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ipc.dsp.DebugProtocolServer;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.Hook;
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
        Hook callHook = ((GlobalsAccess) globals).figuraExtrass$getCaptureState().getSink();
        if (callHook != null) {
            callHook.marker(name);
        }
    }

    @LuaWhitelist
    public void region(String regionName) {
        Globals globals = ((LuaRuntimeAccessor) runtime).getUserGlobals();
        Hook callHook = ((GlobalsAccess) globals).figuraExtrass$getCaptureState().getSink();
        if (callHook != null) {
            callHook.region(regionName);
        }
    }

    @LuaWhitelist
    public void breakpoint() {
        if (DebugProtocolServer.getInstance() != null) {
            assert DebugProtocolServer.getInternalInterface() != null;
            if (DebugProtocolServer.getInternalInterface().cares(runtime.owner) && DebugProtocolServer.getInternalInterface().doCallStop()) {

            }
        }
    }
}
