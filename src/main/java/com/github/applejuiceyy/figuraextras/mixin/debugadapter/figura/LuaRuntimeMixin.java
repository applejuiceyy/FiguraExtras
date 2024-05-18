package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import com.github.applejuiceyy.figuraextras.ipc.dsp.DebugProtocolServer;
import net.minecraft.nbt.ListTag;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.luaj.vm2.Globals;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = FiguraLuaRuntime.class, remap = false)
public class LuaRuntimeMixin {
    @Shadow
    @Final
    public Avatar owner;

    @Shadow
    @Final
    private Globals userGlobals;

    @Inject(method = "initializeScript", at = @At(value = "INVOKE", target = "Lorg/luaj/vm2/Globals;load(Ljava/lang/String;Ljava/lang/String;)Lorg/luaj/vm2/LuaValue;"))
    void notifyDA(String str, CallbackInfoReturnable<Varargs> cir) {
        if (DebugProtocolServer.getInternalInterface() != null && DebugProtocolServer.getInternalInterface().cares(owner)) {
            DebugProtocolServer.getInternalInterface().scriptInitializing(str);
        }

    }

    @Inject(method = "error", at = @At(value = "HEAD"))
    void error(Throwable e, CallbackInfo ci) {
        if (DebugProtocolServer.getInternalInterface() != null && DebugProtocolServer.getInternalInterface().cares(owner)) {
            DebugProtocolServer.getInternalInterface().avatarErrored(e);
        }
    }

    @Inject(method = "init", at = @At(value = "FIELD", target = "Lorg/figuramc/figura/avatar/Avatar;luaRuntime:Lorg/figuramc/figura/lua/FiguraLuaRuntime;", shift = At.Shift.AFTER))
    void aboutToStart(ListTag autoScripts, CallbackInfoReturnable<Boolean> cir) {
        if (DebugProtocolServer.getInternalInterface() != null && DebugProtocolServer.getInternalInterface().cares(owner)) {
            DebugProtocolServer.getInternalInterface().avatarBooting(owner, userGlobals);
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Map;putAll(Ljava/util/Map;)V", shift = At.Shift.AFTER))
    void moreStart(Avatar avatar, Map<String, String> scripts, CallbackInfo ci) {
        if (DebugProtocolServer.getInternalInterface() != null && DebugProtocolServer.getInternalInterface().cares(avatar)) {
            DebugProtocolServer.getInternalInterface().luaRuntimeBooting((FiguraLuaRuntime) (Object) this);
        }
    }
}
