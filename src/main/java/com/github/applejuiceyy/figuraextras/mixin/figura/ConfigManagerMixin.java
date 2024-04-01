package com.github.applejuiceyy.figuraextras.mixin.figura;

import org.figuramc.figura.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ConfigManager.class, remap = false)
public class ConfigManagerMixin {
    @Inject(
            method = "init",
            at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/config/Configs;init()V", shift = At.Shift.AFTER)
    )
    private static void a(CallbackInfo ci) {
        try {
            Class.forName("com.github.applejuiceyy.figuraextras.FiguraExtras");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
