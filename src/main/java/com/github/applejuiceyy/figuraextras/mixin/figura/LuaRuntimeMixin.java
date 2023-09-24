package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.lua.DebuggerAPI;
import com.github.applejuiceyy.figuraextras.tech.captures.ActiveOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.CaptureOpportunity;
import com.github.applejuiceyy.figuraextras.tech.captures.SecondaryCallHook;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.LuaTypeManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

@Mixin(FiguraLuaRuntime.class)
public abstract class LuaRuntimeMixin implements LuaRuntimeAccess {
    @Shadow
    public abstract void setGlobal(String name, Object obj);

    @Shadow
    @Final
    public LuaTypeManager typeManager;
    @Shadow
    @Final
    private Globals userGlobals;
    @Unique
    HashMap<Object, CaptureOpportunity> captures = new HashMap<>();

    @Override
    public HashMap<Object, CaptureOpportunity> figuraExtrass$getNoticedPotentialCaptures() {
        return captures;
    }

    @Inject(method = "loadExtraLibraries", at = @At("TAIL"))
    void e(CallbackInfo ci) {
        setGlobal("debugger", typeManager.javaToLua(new DebuggerAPI((FiguraLuaRuntime) (Object) this)));
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/lua/FiguraLuaRuntime;setInstructionLimit(I)V", shift = At.Shift.AFTER))
    void e(Object toRun, Avatar.Instructions limit, Object[] args, CallbackInfoReturnable<Varargs> cir) {

        CaptureOpportunity current;
        if (!captures.containsKey(toRun)) {
            current = new CaptureOpportunity();
            current.name = toRun.toString();
            captures.put(toRun, current);
        } else {
            current = captures.get(toRun);
            Globals globals = userGlobals;
            SecondaryCallHook capture = ((GlobalsAccess) globals).figuraExtrass$getCurrentCapture();
            ActiveOpportunity<?> thing = ((GlobalsAccess) globals).figuraExtrass$getCurrentlySearchingForCapture();


            if (capture == null && thing != null) {
                CaptureOpportunity opportunity = thing.opportunity();
                if (opportunity == current) {
                    ((GlobalsAccess) globals).figuraExtrass$setCurrentCapture(thing.thing());
                    ((GlobalsAccess) globals).figuraExtrass$setCurrentlySearchingForCapture(null);
                }
            }
        }
        captures.get(toRun).mostRecentCallMillis = System.currentTimeMillis();
    }

    @Inject(method = "run", at = @At(value = "RETURN"))
    void i(Object toRun, Avatar.Instructions limit, Object[] args, CallbackInfoReturnable<Varargs> cir) {
        SecondaryCallHook capture = ((GlobalsAccess) userGlobals).figuraExtrass$getCurrentCapture();
        if (capture != null) {
            capture.end();
            ((GlobalsAccess) userGlobals).figuraExtrass$setCurrentCapture(null);
        }
    }
}
