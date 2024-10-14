package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.UserDataAccess;
import com.github.applejuiceyy.figuraextras.fsstorage.Bucket;
import com.github.applejuiceyy.figuraextras.fsstorage.CommonOps;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.apache.commons.codec.binary.Hex;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.UserData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Mixin(value = UserData.class, remap = false)
public class UserDataMixin implements UserDataAccess {
    @Shadow
    @Final
    public UUID id;
    @Unique
    public CompoundTag guestNbt;

    @Inject(method = "loadAvatar", at = @At("HEAD"), cancellable = true)
    void verify(CompoundTag n, CallbackInfo ci, @Local(argsOnly = true) LocalRef<CompoundTag> nbtVar) {
        if (!id.equals(FiguraMod.getLocalPlayerUUID())) {
            return;
        }

        CompoundTag nbt = nbtVar.get();

        if (FiguraExtras.signAvatars.value > 1) {
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

        if (nbt.contains("figura-extras", Tag.TAG_COMPOUND)) {
            CompoundTag compound = nbt.getCompound("figura-extras");
            if (compound.contains("host-counterpart", Tag.TAG_BYTE_ARRAY)) {
                byte[] hash = compound.getByteArray("host-counterpart");
                Bucket bucket = FiguraExtras.hostSideStorage.getBucket(Hex.encodeHexString(hash));
                if (bucket != null) {
                    byte[] bytes = bucket.get(FiguraExtras.HOST_AVATAR);
                    bucket.set(CommonOps.TIME, Instant.now());
                    try {
                        nbtVar.set(NbtIo.readCompressed(new ByteArrayInputStream(bytes)));
                        guestNbt = n;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @ModifyExpressionValue(method = "loadAvatar", at = @At(value = "NEW", target = "(Ljava/util/UUID;)Lorg/figuramc/figura/avatar/Avatar;"))
    Avatar setGuestNbt(Avatar original) {
        if (guestNbt != null) {
            ((AvatarAccess) original).figuraExtrass$setGuestNbt(guestNbt);
            guestNbt = null;
        }
        return original;
    }

    @Override
    public void figuraExtrass$setFutureAvatarGuestNbt(CompoundTag tag) {
        guestNbt = tag;
    }
}
