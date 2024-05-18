package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import org.figuramc.figura.backend2.websocket.S2CMessageHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(value = S2CMessageHandler.class, remap = false)
public class S2CMessageHandlerMixin {
    @Inject(method = "toast", at = @At("HEAD"), cancellable = true)
    private static void cancel(ByteBuffer bytes, CallbackInfo ci) {
        if (FiguraExtras.disableServerToasts.value) {
            ci.cancel();
        }
    }
}
