package com.github.applejuiceyy.figuraextras.mixin.figura;


import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.luaj.vm2.Globals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FiguraLuaRuntime.class, remap = false)
public interface LuaRuntimeAccessor {
    @Accessor
    Globals getUserGlobals();
}
