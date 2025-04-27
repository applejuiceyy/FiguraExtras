package com.github.applejuiceyy.figuraextras.mixin.figura.gui;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.gui.widgets.AvatarInfoWidget;
import org.figuramc.figura.utils.MathUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AvatarInfoWidget.class, remap = false)
public class AvatarInfoMixin {

    @Shadow
    @Final
    private List<Component> values;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;set(ILjava/lang/Object;)Ljava/lang/Object;", ordinal = 2, shift = At.Shift.AFTER))
    void modifySize(CallbackInfo ci, @Local Avatar avatar) {
        if (((AvatarAccess) avatar).figuraExtras$getOtherNbt() != null) {
            AvatarAccess.Side side = ((AvatarAccess) avatar).figuraExtras$getCurrentSide();
            int current = avatar.fileSize;
            int other = ((AvatarAccess) avatar).figuraExtras$getOtherFileSize();

            String host = MathUtils.asFileSize(side == AvatarAccess.Side.HOST ? current : other) + "(H)";
            String guest = MathUtils.asFileSize(side == AvatarAccess.Side.GUEST ? current : other) + "(G)";

            ChatFormatting hostF = side == AvatarAccess.Side.HOST ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
            ChatFormatting guestF = side == AvatarAccess.Side.GUEST ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;

            values.set(2, Component.empty()
                .append(Component.literal(host).withStyle(hostF))
                .append(" ")
                .append(Component.literal(guest).withStyle(guestF)));
        }
    }
}
