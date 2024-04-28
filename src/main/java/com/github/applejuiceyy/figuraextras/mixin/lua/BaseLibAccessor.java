package com.github.applejuiceyy.figuraextras.mixin.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.BaseLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseLib.class)
public interface BaseLibAccessor {
    @Accessor
    Globals getGlobals();
}
