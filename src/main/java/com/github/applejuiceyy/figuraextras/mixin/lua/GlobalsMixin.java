package com.github.applejuiceyy.figuraextras.mixin.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.tech.captures.CaptureState;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Globals.class, remap = false)
public class GlobalsMixin implements GlobalsAccess {
    @Unique
    CaptureState captureState = new CaptureState((Globals) (Object) this);
    @Unique
    LuaTable offTheShelfDebugLib;

    @Override
    public LuaTable figuraExtras$getOffTheShelfDebugLib() {
        return offTheShelfDebugLib;
    }

    @Override
    public void figuraExtras$setOffTheShelfDebugLib(LuaTable table) {
        offTheShelfDebugLib = table;
    }

    @Override
    public CaptureState figuraExtras$getCaptureState() {
        return captureState;
    }
}
