package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import com.github.applejuiceyy.figuraextras.ipc.IPCManager;
import com.neovisionaries.ws.client.WebSocket;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.backend2.websocket.S2CMessageHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.util.UUID;

@Mixin(value = NetworkStuff.class, remap = false)
public class NetworkStuffMixin {
    @Inject(method = "connectWS", at = @At("HEAD"), cancellable = true)
    static private void cancel(String token, CallbackInfo ci) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            S2CMessageHandler.handle(ByteBuffer.wrap(new byte[]{0}));
            ci.cancel();
        }
    }

    @Inject(method = "checkWS", at = @At("HEAD"), cancellable = true)
    static private void override(CallbackInfoReturnable<Boolean> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(NetworkStuff.backendStatus == 3);
        }
    }

    @Redirect(method = "sendPing", at = @At(value = "INVOKE", target = "Lcom/neovisionaries/ws/client/WebSocket;sendBinary([B)Lcom/neovisionaries/ws/client/WebSocket;"))
    static private WebSocket overridePing(WebSocket instance, byte[] message) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            IPCManager.INSTANCE.getC2CServer().getBackend().ping(message);
            return instance;
        } else {
            return instance.sendBinary(message);
        }
    }

    @Redirect(method = "lambda$subscribe$20", at = @At(value = "INVOKE", target = "Lcom/neovisionaries/ws/client/WebSocket;sendBinary([B)Lcom/neovisionaries/ws/client/WebSocket;"))
    static private WebSocket overrideSub(WebSocket instance, byte[] message) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            return instance;
        } else {
            return instance.sendBinary(message);
        }
    }

    @Inject(method = "lambda$subscribe$20", at = @At(value = "INVOKE", target = "Lcom/neovisionaries/ws/client/WebSocket;sendBinary([B)Lcom/neovisionaries/ws/client/WebSocket;"))
    static private void overrideSub(UUID id, WebSocket client, CallbackInfo ci) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend() && IPCManager.INSTANCE.divertBackend.isBackendConnected()) {
            IPCManager.INSTANCE.getC2CServer().getBackend().subscribe(id.toString());
        }
    }

    @Redirect(method = "lambda$unsubscribe$21", at = @At(value = "INVOKE", target = "Lcom/neovisionaries/ws/client/WebSocket;sendBinary([B)Lcom/neovisionaries/ws/client/WebSocket;"))
    static private WebSocket overrideUnSub(WebSocket instance, byte[] message) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend() && IPCManager.INSTANCE.divertBackend.isBackendConnected()) {
            return instance;
        } else {
            return instance.sendBinary(message);
        }
    }

    @Inject(method = "lambda$unsubscribe$21", at = @At(value = "INVOKE", target = "Lcom/neovisionaries/ws/client/WebSocket;sendBinary([B)Lcom/neovisionaries/ws/client/WebSocket;"))
    static private void overrideUnSub(UUID id, WebSocket client, CallbackInfo ci) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend() && IPCManager.INSTANCE.divertBackend.isBackendConnected()) {
            IPCManager.INSTANCE.getC2CServer().getBackend().unsubscribe(id.toString());
        }
    }

    @Inject(method = "isConnected", at = @At(value = "HEAD"), cancellable = true)
    static private void overrideConnected(CallbackInfoReturnable<Boolean> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(NetworkStuff.backendStatus == 3);
        }
    }

    @Inject(method = "checkUUID", at = @At(value = "HEAD"), cancellable = true)
    static private void overrideUUID(CallbackInfoReturnable<Boolean> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "disconnect", at = @At(value = "HEAD"))
    static private void disconnect(String reason, CallbackInfo ci) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend() && IPCManager.INSTANCE.divertBackend.isBackendConnected()) {
            IPCManager.INSTANCE.divertBackend.disconnectDivertedBackend();
        }
    }
}
