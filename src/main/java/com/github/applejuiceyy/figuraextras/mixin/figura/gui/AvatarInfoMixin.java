package com.github.applejuiceyy.figuraextras.mixin.figura.gui;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.gui.widgets.AvatarInfoWidget;
import org.figuramc.figura.utils.MathUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AvatarInfoWidget.class)
public class AvatarInfoMixin {

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 2))
    MutableComponent modifySize(MutableComponent original, @Local Avatar avatar) {
        if (((AvatarAccess) avatar).figuraExtrass$getGuestNbt() != null) {
            String host = MathUtils.asFileSize(avatar.fileSize);
            String guest = MathUtils.asFileSize(((AvatarAccess) avatar).figuraExtrass$getGuestFileSize());
            return Component.literal(host + "(H) " + guest + "(G)");
        }
        return original;
    }
}
