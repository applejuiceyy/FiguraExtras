package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.CommonOps;
import com.github.applejuiceyy.figuraextras.ipc.IPCManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.neovisionaries.ws.client.WebSocket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.apache.commons.codec.binary.Hex;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.backend2.websocket.S2CMessageHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
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

    @ModifyExpressionValue(method = "uploadAvatar", at = @At(value = "FIELD", target = "Lorg/figuramc/figura/avatar/Avatar;nbt:Lnet/minecraft/nbt/CompoundTag;", ordinal = 1), remap = true)
    private static CompoundTag uploadGuestNbt(CompoundTag original, @Local(argsOnly = true) Avatar avatar) {
        CompoundTag guestNbt = ((AvatarAccess) avatar).figuraExtrass$getGuestNbt();
        if (guestNbt != null) {

            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
                NbtIo.writeCompressed(avatar.nbt, outputStream);
                byte[] bytes = outputStream.toByteArray();
                byte[] array = guestNbt.getCompound("figura-extras").getByteArray("host-counterpart");
                String name = Hex.encodeHexString(array);
                Bucket bucket = FiguraExtras.hostSideStorage.getBucket(name);
                if (bucket != null) {
                    bucket.set(FiguraExtras.HOST_AVATAR, bytes);
                    bucket.set(CommonOps.TIME, Instant.now());
                } else {
                    FiguraExtras.hostSideStorage
                            .createBucket(name)
                            .data(CommonOps.TIME, Instant.now())
                            .data(FiguraExtras.HOST_AVATAR, bytes)
                            .create();
                }
            } catch (IOException e) {
                FiguraExtras.sendBrandedMessage("Host Splitting Error", style -> style.withColor(ChatFormatting.RED), "An error has happened while managing host splitting: " + e.getMessage());
            }
            return guestNbt;
        }
        return original;
    }
}
