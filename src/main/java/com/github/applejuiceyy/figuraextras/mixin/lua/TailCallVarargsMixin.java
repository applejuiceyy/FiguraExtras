package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import org.luaj.vm2.TailcallVarargs;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TailcallVarargs.class, remap = false)
public class TailCallVarargsMixin {
    @Inject(method = "eval", at = @At(value = "INVOKE", target = "Lorg/luaj/vm2/LuaValue;onInvoke(Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;"))
    void e(CallbackInfoReturnable<Varargs> cir) {
        LuaDuck.currentCallType = LuaDuck.CallType.TAIL;
    }

    @Inject(method = "eval", at = @At(value = "RETURN"))
    void c(CallbackInfoReturnable<Varargs> cir) {
        LuaDuck.currentCallType = LuaDuck.CallType.NORMAL;
    }
}
