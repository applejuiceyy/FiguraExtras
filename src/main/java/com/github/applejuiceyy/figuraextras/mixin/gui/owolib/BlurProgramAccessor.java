package com.github.applejuiceyy.figuraextras.mixin.gui.owolib;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.wispforest.owo.shader.BlurProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlurProgram.class)
public interface BlurProgramAccessor {
    @Accessor
    RenderTarget getInput();

    @Accessor
    void setInput(RenderTarget target);
}
