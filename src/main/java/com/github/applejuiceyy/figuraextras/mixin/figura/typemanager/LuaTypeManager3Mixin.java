package com.github.applejuiceyy.figuraextras.mixin.figura.typemanager;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaTypeManagerAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaRuntimeAccessor;
import com.github.applejuiceyy.figuraextras.tech.captures.Hook;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaTypeManager;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.reflect.Method;

@Mixin(targets = "org/figuramc/figura/lua/LuaTypeManager$3", remap = false)
public abstract class LuaTypeManager3Mixin {
    @Shadow
    @Final
    LuaTypeManager this$0;

    @Shadow
    @Final
    Method val$method;

    @Unique
    boolean wrap = true;

    @Shadow
    public abstract Varargs invoke(Varargs args);

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "invoke", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    void outOfJava(Varargs args, CallbackInfoReturnable<Varargs> cir, int i, Object result) {
        Avatar avatar = ((LuaTypeManagerAccess) this$0).figuraExtrass$getAvatar();
        if (avatar != null) {
            Hook hook = ((GlobalsAccess) ((LuaRuntimeAccessor) avatar.luaRuntime).getUserGlobals())
                    .figuraExtrass$getCaptureState().getSink();

            if (hook != null) {
                hook.outOfJavaFunction(args, val$method, result, LuaDuck.ReturnType.NORMAL);
            }
        }
    }

    @Inject(method = "invoke", at = @At("HEAD"), cancellable = true)
    void wrap(Varargs args, CallbackInfoReturnable<Varargs> cir) {
        if (wrap) {
            wrap = false;
            try {
                cir.setReturnValue(invoke(args));
            } catch (Throwable e) {
                Avatar avatar = ((LuaTypeManagerAccess) this$0).figuraExtrass$getAvatar();
                if (avatar != null) {
                    Hook hook = ((GlobalsAccess) ((LuaRuntimeAccessor) avatar.luaRuntime).getUserGlobals())
                            .figuraExtrass$getCaptureState().getSink();

                    if (hook != null) {
                        hook.outOfJavaFunction(args,
                                val$method,
                                e.getCause() instanceof LuaError l ? l : new LuaError(e.getCause()),
                                LuaDuck.ReturnType.ERROR
                        );
                    }
                }
                throw e;
            } finally {
                wrap = true;
            }
        } else {
            LuaDuck.CallType type = LuaDuck.currentCallType;
            LuaDuck.currentCallType = LuaDuck.CallType.NORMAL;

            Avatar avatar = ((LuaTypeManagerAccess) this$0).figuraExtrass$getAvatar();
            if (avatar != null) {
                Hook hook = ((GlobalsAccess) ((LuaRuntimeAccessor) avatar.luaRuntime).getUserGlobals())
                        .figuraExtrass$getCaptureState().getSink();

                if (hook != null) {
                    hook.intoJavaFunction(args, val$method, type);
                }
            }
        }
    }
}
