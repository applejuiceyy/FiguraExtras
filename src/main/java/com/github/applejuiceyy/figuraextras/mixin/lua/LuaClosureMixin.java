package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import com.github.applejuiceyy.figuraextras.tech.captures.Hook;
import org.luaj.vm2.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = LuaClosure.class, remap = false)
public abstract class LuaClosureMixin {
    @Shadow
    @Final
    Globals globals;

    @Unique
    LuaDuck.ReturnType currentType = LuaDuck.ReturnType.NORMAL;
    @Unique
    boolean run = true;

    @Shadow
    protected abstract Varargs execute(LuaValue[] stack, Varargs varargs);

    @Inject(method = "execute([Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At(value = "HEAD"), cancellable = true)
    void bootstrap(LuaValue[] stack, Varargs varargs, CallbackInfoReturnable<Varargs> cir) {
        if (run) {
            run = false;
            Varargs ret;
            try {
                ret = execute(stack, varargs);
                cir.setReturnValue(ret);
                outOfFunction(stack, varargs, ret, currentType);
            } catch (LuaError err) {
                outOfFunction(stack, varargs, err, LuaDuck.ReturnType.ERROR);
                throw err;
            } finally {
                currentType = LuaDuck.ReturnType.NORMAL;
            }
            return;
        }
        run = true;
    }


    @Inject(method = "execute([Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At(value = "INVOKE", target = "Lorg/luaj/vm2/lib/DebugLib;onCall(Lorg/luaj/vm2/LuaClosure;Lorg/luaj/vm2/Varargs;[Lorg/luaj/vm2/LuaValue;)V", ordinal = 0, shift = At.Shift.BY, by = 1))
    void init(LuaValue[] stack, Varargs varargs, CallbackInfoReturnable<Varargs> cir) {
        LuaDuck.CallType type = LuaDuck.currentCallType;
        LuaDuck.currentCallType = LuaDuck.CallType.NORMAL;


        if (globals != null) {
            GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
            Hook capture = globalsAccess.figuraExtrass$getCaptureState().getSink();
            if (capture != null) {
                String possibleName = null;

                if (globals.debuglib != null) {
                    LuaTable debugLib = ((GlobalsAccess) globals).figuraExtrass$getOffTheShelfDebugLib();
                    LuaTable o = debugLib.get("getinfo").invoke(LuaValue.varargsOf(LuaValue.valueOf(1), LuaValue.valueOf("n"))).arg1().checktable();
                    possibleName = o.get("name").checkjstring();
                    possibleName = possibleName.equals("?") ? null : possibleName;
                }

                capture.intoFunction((LuaClosure) (Object) this, varargs, stack, type, possibleName);
            }
        }
    }

    @Inject(
            method = "execute([Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;",
            at = @At(value = "FIELD", target = "Lorg/luaj/vm2/LuaClosure;globals:Lorg/luaj/vm2/Globals;", ordinal = 3),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    void instruction(LuaValue[] stack, Varargs varargs, CallbackInfoReturnable<Varargs> cir, int pc, int top, Varargs v, int[] code, LuaValue[] k, UpValue[] openups) {
        if (globals != null) {
            GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
            Hook capture = globalsAccess.figuraExtrass$getCaptureState().getSink();
            if (capture != null) {
                capture.instruction((LuaClosure) (Object) this, varargs, stack, code[pc], pc);
            }
        }
    }

    @Inject(
            method = "execute([Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;",
            at = @At(
                    value = "NEW",
                    target = "(Lorg/luaj/vm2/LuaValue;Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/TailcallVarargs;"
            )
    )
    void tailcall(LuaValue[] stack, Varargs varargs, CallbackInfoReturnable<Varargs> cir) {
        currentType = LuaDuck.ReturnType.TAIL;
    }

    @Unique
    void outOfFunction(LuaValue[] stack, Varargs varargs, Object returns, LuaDuck.ReturnType returnType) {
        if (globals != null) {
            GlobalsAccess globalsAccess = ((GlobalsAccess) globals);
            Hook capture = globalsAccess.figuraExtrass$getCaptureState().getSink();
            if (capture != null) {
                capture.outOfFunction((LuaClosure) (Object) this, varargs, stack, returns, returnType);
            }
        }
    }
}
