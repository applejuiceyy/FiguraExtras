package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.UserData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mixin(value = UserData.class, remap = false)
public class UserDataMixin {
    @Shadow
    @Final
    public UUID id;

    @Inject(method = "loadAvatar", at = @At("HEAD"), cancellable = true)
    void verify(CompoundTag nbt, CallbackInfo ci) {
        if (!id.equals(FiguraMod.getLocalPlayerUUID())) {
            return;
        }
        if (FiguraExtras.signAvatars.value <= 1) {
            return;
        }
        boolean verified = false;
        if (nbt.contains("figura-extras", Tag.TAG_COMPOUND)) {
            CompoundTag figuraExtras = nbt.getCompound("figura-extras");
            nbt.remove("figura-extras");
            if (figuraExtras.contains("signature", Tag.TAG_BYTE_ARRAY)) {
                byte[] signature = figuraExtras.getByteArray("signature");
                verified = FiguraExtras.avatarSigner.verify(nbt.getAsString().getBytes(StandardCharsets.UTF_8), signature);
            }
            nbt.put("figura-extras", figuraExtras);
        }
        if (!verified) {
            String bake = " has not been signed in this computer or it has an invalid signature";
            if (FiguraExtras.signAvatars.value == 2) {
                bake = "Your avatar" + bake + ", you can re-upload the avatar to sign it and get rid of the warning";
            } else {
                bake = "Your avatar was not equipped because it" + bake;
            }
            FiguraExtras.sendBrandedMessage(
                    "FiguraExtras",
                    FiguraExtras.signAvatars.value == 2 ? ChatFormatting.YELLOW : ChatFormatting.RED,
                    bake
            );
            if (FiguraExtras.signAvatars.value == 3) {
                ci.cancel();
            }
        }
    }
}
