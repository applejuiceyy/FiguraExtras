package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import com.github.applejuiceyy.figuraextras.ipc.IPCManager;
import org.figuramc.figura.backend2.websocket.FiguraWebSocketAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FiguraWebSocketAdapter.class, remap = false)
public class FiguraWebSocketAdapterMixin {
    @Inject(method = "handleClose", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/backend2/NetworkStuff;disconnect(Ljava/lang/String;)V"), cancellable = true)
    void cancelLateDisconnect(int code, String reason, CallbackInfo ci) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            ci.cancel();
        }
    }
}
