package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import com.github.applejuiceyy.figuraextras.vscode.dsp.DebugProtocolServer;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalAvatarLoader.class, remap = false)
public class LocalAvatarLoaderMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/avatar/AvatarManager;loadLocalAvatar(Ljava/nio/file/Path;)V"), cancellable = true)
    static private void reloading(CallbackInfo ci) {
        if (DebugProtocolServer.getInternalInterface() != null) {
            DebugProtocolServer.getInternalInterface().avatarReloading();
            ci.cancel();
        }
    }
}
