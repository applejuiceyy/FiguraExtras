package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.ipc.IPCManager;
import org.figuramc.figura.utils.EntityUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = EntityUtils.class, remap = false)
public class EntityUtilsMixin {
    @Inject(method = "checkInvalidPlayer", at = @At("HEAD"), cancellable = true)
    static private void override(UUID id, CallbackInfoReturnable<Boolean> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(false);
        }
    }
}
