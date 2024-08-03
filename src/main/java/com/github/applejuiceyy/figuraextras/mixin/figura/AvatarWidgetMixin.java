package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.avatar.AvatarWidget;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AvatarWidget.class, remap = false)
public class AvatarWidgetMixin {
    @Inject(
            method = "<init>",
            at = @At(
                    value = "TAIL"
            )
    )
    void a(int depth, int width, LocalAvatarFetcher.AvatarPath avatar, AvatarList parent, CallbackInfo ci) {
        ContextMenu menu = ((AvatarWidgetAccessor) this).getContextMenu();
        menu.addAction(Component.literal("Open with " + FiguraExtras.progName.value), null, button -> {
            try {
                String cmd = FiguraExtras.progCmd.value.replace("$folder", avatar.getPath().toString());
                if (Util.getPlatform() == Util.OS.WINDOWS) {
                    Runtime.getRuntime().exec("cmd.exe /c " + cmd);
                } else {
                    Runtime.getRuntime().exec("/bin/sh -c " + cmd);
                }

                FiguraToast.sendToast("On your way");
            } catch (Exception e) {
                FiguraToast.sendToast("Couldn't open", e.getMessage(), FiguraToast.ToastType.ERROR);
            }
        });
    }
}
