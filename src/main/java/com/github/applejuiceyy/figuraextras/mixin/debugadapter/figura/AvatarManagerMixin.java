package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import com.github.applejuiceyy.figuraextras.vscode.dsp.DebugProtocolServer;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = AvatarManager.class, remap = false)
public class AvatarManagerMixin {
    @Shadow
    public static Avatar getAvatarForPlayer(UUID player) {
        throw new IllegalStateException("Not injected");
    }

    @Inject(method = "clearAvatars", at = @At("HEAD"))
    static private void clearing(UUID id, CallbackInfo ci) {

        DebugProtocolServer.DAInternalInterface internalInterface = DebugProtocolServer.getInternalInterface();
        if (internalInterface != null) {
            Avatar avatar = getAvatarForPlayer(id);
            if (avatar != null && internalInterface.cares(avatar)) {
                internalInterface.avatarClearing();
            }
        }
    }
}
