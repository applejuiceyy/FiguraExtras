package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import org.figuramc.figura.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConfigManager.class)
public class ConfigManagerMixin {
    @Inject(
            method = "init",
            at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/config/Configs;init()V", shift = At.Shift.AFTER)
    )
    static void a(CallbackInfo ci) {
        noop(FiguraExtras.class);
    }

    @Unique
    static void noop(Object ignore) {}
}
