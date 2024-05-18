package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import com.github.applejuiceyy.figuraextras.ducks.statics.AuthHandlerDuck;
import com.github.applejuiceyy.figuraextras.util.Util;
import org.figuramc.figura.backend2.AuthHandler;
import org.figuramc.figura.backend2.NetworkStuff;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AuthHandler.class, remap = false)
public class AuthHandlerMixin {
    @Inject(method = "lambda$auth$0", at = @At("HEAD"), cancellable = true)
    static private void override(boolean reAuth, CallbackInfo ci) {
        if (AuthHandlerDuck.isDiverting()) {
            if (reAuth || !NetworkStuff.isConnected()) {
                Util.after(() -> NetworkStuffAccessor.invokeAuthSuccess(""), 1000);
            }

            ci.cancel();
        }
    }
}
