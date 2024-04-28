package com.github.applejuiceyy.figuraextras.mixin.figura.lua;

import com.github.applejuiceyy.figuraextras.lua.DebuggerAPI;
import com.github.applejuiceyy.figuraextras.lua.DelayedResponse;
import org.figuramc.figura.lua.FiguraAPIManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(value = FiguraAPIManager.class, remap = false)
public class FiguraAPIManagerMixin {
    @Shadow
    @Final
    public static Set<Class<?>> WHITELISTED_CLASSES;

    static {
        WHITELISTED_CLASSES.add(DebuggerAPI.class);
        WHITELISTED_CLASSES.add(DelayedResponse.class);
    }
}
