package com.github.applejuiceyy.figuraextras.mixin.figura.gui;

import com.github.applejuiceyy.figuraextras.ipc.IPCManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.gui.widgets.StatusWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusWidget.class)
public class StatusWidgetMixin {
    @Inject(method = "getStatusIcon", at = @At("HEAD"), cancellable = true)
    void symbol(int type, CallbackInfoReturnable<MutableComponent> cir) {
        if (type == 3) {
            if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend() && IPCManager.INSTANCE.divertBackend.isBackendConnected()) {
                cir.setReturnValue(Component.literal("L").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    @Inject(method = "getTooltipFor", at = @At("HEAD"), cancellable = true)
    void tooltip(int type, CallbackInfoReturnable<MutableComponent> cir) {
        if (type == 3) {
            if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend() && IPCManager.INSTANCE.divertBackend.isBackendConnected()) {
                cir.setReturnValue(Component.literal("Local backend").withStyle(ChatFormatting.GREEN));
            }
        }
    }
}
