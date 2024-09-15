package com.github.applejuiceyy.figuraextras.mixin.component;

import com.github.applejuiceyy.figuraextras.ducks.TextComponentTagVisitorAccess;
import com.google.common.base.Strings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextComponentTagVisitor.class)
public class TextComponentTagVisitorMixin implements TextComponentTagVisitorAccess {

    @Shadow
    @Final
    private String indentation;
    @Shadow
    @Final
    private int depth;
    @Unique
    private int limit = Integer.MAX_VALUE;

    @Inject(
            method = "visitByteArray",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 2, shift = At.Shift.AFTER)
    )
    void theBytes(ByteArrayTag element, CallbackInfo ci, @Local(ordinal = 0) MutableComponent component, @Local LocalIntRef i) {
        if (limit <= i.get()) {
            component.append(Component.literal("... some left ...").withStyle(ChatFormatting.GRAY));
            i.set(Integer.MAX_VALUE - 1);
        }
    }

    @Inject(
            method = "visitIntArray",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 2, shift = At.Shift.AFTER)
    )
    void theInts(IntArrayTag element, CallbackInfo ci, @Local(ordinal = 0) MutableComponent component, @Local LocalIntRef i) {
        if (limit <= i.get()) {
            component.append(Component.literal("... some left ...").withStyle(ChatFormatting.GRAY));
            i.set(Integer.MAX_VALUE - 1);
        }
    }

    @Inject(
            method = "visitLongArray",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 2, shift = At.Shift.AFTER)
    )
    void theLongs(LongArrayTag element, CallbackInfo ci, @Local(ordinal = 0) MutableComponent component, @Local LocalIntRef i) {
        if (limit <= i.get()) {
            component.append(Component.literal("... some left ...").withStyle(ChatFormatting.GRAY));
            i.set(Integer.MAX_VALUE - 1);
        }
    }

    @Inject(
            method = "visitCompound",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;isEmpty()Z", ordinal = 0)
    )
    void compoundsStart(CompoundTag compound, CallbackInfo ci, @Share("loops") LocalIntRef i) {
        i.set(0);
    }

    @Inject(
            method = "visitCompound",
            at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;")
    )
    void compoundsStep(CompoundTag compound, CallbackInfo ci, @Share("loops") LocalIntRef i) {
        i.set(i.get() + 1);
    }

    @Inject(
            method = "visitCompound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 4, shift = At.Shift.AFTER)
    )
    void theCompounds(CompoundTag compound, CallbackInfo ci, @Local(ordinal = 1) MutableComponent component, @Share("loops") LocalIntRef i) {
        if (limit <= i.get()) {
            component.append(Component.literal(Strings.repeat(indentation, depth + 1)).append("... some left ...").withStyle(ChatFormatting.GRAY));
            i.set(Integer.MAX_VALUE - 1);
        }
    }

    @ModifyExpressionValue(
            method = "visitCompound",
            at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0)
    )
    boolean stopCompounds(boolean original, @Share("loops") LocalIntRef i) {
        return original && limit > i.get();
    }


    @ModifyExpressionValue(
            method = "visitCompound",
            at = @At(value = "NEW", args = "class=net/minecraft/nbt/TextComponentTagVisitor")
    )
    TextComponentTagVisitor compounds(TextComponentTagVisitor original) {
        ((TextComponentTagVisitorAccess) original).figuraExtrass$setElementLimit(limit);
        return original;
    }

    @ModifyExpressionValue(
            method = "visitList",
            at = @At(value = "NEW", args = "class=net/minecraft/nbt/TextComponentTagVisitor")
    )
    TextComponentTagVisitor lists(TextComponentTagVisitor original) {
        ((TextComponentTagVisitorAccess) original).figuraExtrass$setElementLimit(limit);
        return original;
    }

    @Inject(
            method = "visitList",
            at = {
                    @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0, shift = At.Shift.AFTER),
            }
    )
    void theLists1(ListTag list, CallbackInfo ci, @Local(ordinal = 0) MutableComponent component, @Local LocalIntRef i) {
        if (limit <= i.get()) {
            component.append(Component.literal("... some left ...").withStyle(ChatFormatting.GRAY));
            i.set(Integer.MAX_VALUE - 1);
        }
    }

    @Inject(
            method = "visitList",
            at = {
                    @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;append(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = 4, shift = At.Shift.AFTER)
            }
    )
    void theLists2(ListTag list, CallbackInfo ci, @Local(ordinal = 1) MutableComponent component, @Local LocalIntRef i) {
        if (limit <= i.get()) {
            component.append(Component.literal(Strings.repeat(indentation, depth + 1)).append("... some left ...").withStyle(ChatFormatting.GRAY));
            i.set(Integer.MAX_VALUE - 1);
        }
    }


    @Override
    public void figuraExtrass$setElementLimit(int limit) {
        this.limit = limit;
    }
}
