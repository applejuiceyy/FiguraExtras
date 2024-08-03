package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(FiguraLuaRuntime.class)
public interface LuaRuntimeAccessor {
    @Accessor
    Map<String, Varargs> getLoadedScripts();

    @Accessor
    Map<String, String> getScripts();
}
