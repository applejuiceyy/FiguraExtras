package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.ducks.InstructionsAccess;
import org.figuramc.figura.avatar.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.IntConsumer;

@Mixin(Avatar.Instructions.class)
public abstract class InstructionsMixin implements InstructionsAccess {
    @Shadow
    public abstract int getTotal();

    ArrayList<IntConsumer> hooks = new ArrayList<>();

    @Override
    public Runnable addHook(IntConsumer result) {
        hooks.add(result);
        return () -> hooks.remove(result);
    }

    @Inject(method = "reset", at = @At("HEAD"))
    void callHooks(int ignored, CallbackInfo ci) {
        for (IntConsumer hook : hooks) {
            hook.accept(getTotal());
        }
    }
}
