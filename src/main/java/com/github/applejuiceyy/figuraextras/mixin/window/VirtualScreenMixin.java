package com.github.applejuiceyy.figuraextras.mixin.window;

import com.github.applejuiceyy.figuraextras.ducks.VirtualScreenAccess;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.renderer.VirtualScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(VirtualScreen.class)
public class VirtualScreenMixin implements VirtualScreenAccess {
    @Unique
    WindowEventHandler currentHandler;

    @Override
    public void figuraExtrass$setEventListener(@Nullable WindowEventHandler handler) {
        currentHandler = handler;
    }

    @ModifyArg(
            method = "newWindow",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V"),
            index = 0
    )
    WindowEventHandler a(WindowEventHandler eventHandler) {
        if (currentHandler != null) {
            return currentHandler;
        }
        return eventHandler;
    }
}
