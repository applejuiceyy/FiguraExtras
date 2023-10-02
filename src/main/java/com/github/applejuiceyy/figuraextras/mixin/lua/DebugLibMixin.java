package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.DebugLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = DebugLib.class, remap = false)
public class DebugLibMixin {
    @Shadow
    Globals globals;

    @Inject(
            method = "call", at = @At(value = "RETURN"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    void e(LuaValue modname, LuaValue env, CallbackInfoReturnable<LuaValue> cir, LuaTable debug) {
        ((GlobalsAccess) globals).figuraExtrass$setOffTheShelfDebugLib(debug);
    }
}
