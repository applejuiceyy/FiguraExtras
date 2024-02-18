package com.github.applejuiceyy.figuraextras.mixin.figura;

import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.ImmediateAvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ImmediateAvatarRenderer.class, remap = false)
public interface ImmediateAvatarRendererAccessor {
    @Accessor
    PartCustomization.PartCustomizationStack getCustomizationStack();
}
