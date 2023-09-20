package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.DelayedResponse;
import com.github.applejuiceyy.figuraextras.render.rendertasks.EntityTask;
import org.figuramc.figura.lua.FiguraAPIManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(FiguraAPIManager.class)
public class FiguraAPIManagerMixin {
    @Shadow
    @Final
    public static Set<Class<?>> WHITELISTED_CLASSES;

    static {
        WHITELISTED_CLASSES.add(DelayedResponse.class);
        WHITELISTED_CLASSES.add(EntityTask.class);
    }
}
