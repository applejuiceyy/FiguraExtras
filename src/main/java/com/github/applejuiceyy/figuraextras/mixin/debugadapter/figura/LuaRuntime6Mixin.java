package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import org.apache.commons.io.input.TeeInputStream;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.luaj.vm2.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "org/figuramc/figura/lua/FiguraLuaRuntime$6", remap = false)
public class LuaRuntime6Mixin {
    @Shadow
    @Final
    FiguraLuaRuntime val$runtime;

    @Redirect(method = "invoke", at = @At(value = "INVOKE", target = "Lorg/luaj/vm2/Globals;load(Ljava/io/InputStream;Ljava/lang/String;Ljava/lang/String;Lorg/luaj/vm2/LuaValue;)Lorg/luaj/vm2/LuaValue;"))
    LuaValue e(Globals globals, InputStream i, String chunkName, String mode, LuaValue env) {
        InputStream stream;
        byte[] buf;
        try {
            buf = i.readAllBytes();
            stream = new ByteArrayInputStream(buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LuaValue loaded = globals.load(stream, chunkName, mode, env);

        if (loaded instanceof LuaClosure closure) {
            ((LuaRuntimeAccess) val$runtime).figuraExtrass$newDynamicLoad(closure.p, new String(buf));
        }
        ;
        return loaded;
    }
}
