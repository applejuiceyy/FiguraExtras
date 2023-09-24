package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LuaClosure.class, remap = false)
public class LuaClosureMixin {
    @Shadow
    @Final
    Globals globals;

    @Inject(method = "execute([Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At(value = "FIELD", target = "Lorg/luaj/vm2/LuaClosure;globals:Lorg/luaj/vm2/Globals;", ordinal = 0))
    void a(LuaValue[] stack, Varargs varargs, CallbackInfoReturnable<Varargs> cir) {
        if (globals != null) {
            GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
            SecondaryCallHook capture = globalsAccess.figuraExtrass$getCurrentCapture();
            if (capture != null) {
                capture.intoFunction((LuaClosure) (Object) this, varargs, stack);
            }
        }
    }

    @Inject(method = "execute([Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At(value = "FIELD", target = "Lorg/luaj/vm2/LuaClosure;globals:Lorg/luaj/vm2/Globals;", ordinal = 3))
    void e(LuaValue[] stack, Varargs varargs, CallbackInfoReturnable<Varargs> cir) {
        if (globals != null) {
            GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
            SecondaryCallHook capture = globalsAccess.figuraExtrass$getCurrentCapture();
            if (capture != null) {
                capture.instruction((LuaClosure) (Object) this, varargs, stack);
            }
        }
    }

    @Inject(method = "execute([Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At(value = "RETURN"))
    void i(LuaValue[] stack, Varargs varargs, CallbackInfoReturnable<Varargs> cir) {
        if (globals != null) {
            GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
            SecondaryCallHook capture = globalsAccess.figuraExtrass$getCurrentCapture();
            if (capture != null) {
                capture.outOfFunction((LuaClosure) (Object) this, varargs, stack);
            }
        }
    }
}