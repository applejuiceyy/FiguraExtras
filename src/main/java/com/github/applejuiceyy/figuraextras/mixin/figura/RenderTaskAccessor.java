package com.github.applejuiceyy.figuraextras.mixin.figura;

import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.rendertasks.RenderTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RenderTask.class, remap = false)
public interface RenderTaskAccessor {
    @Accessor
    FiguraModelPart getParent();
}
