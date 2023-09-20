package com.github.applejuiceyy.figuraextras.mixin.figura.printer;

import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.LuaTypeManager;
import org.luaj.vm2.LuaValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FiguraLuaPrinter.class)
public interface FiguraLuaPrinterAccessor {
    @Invoker
    static MutableComponent invokeGetPrintText(LuaTypeManager typeManager, LuaValue value, boolean hasTooltip, boolean quoteStrings) {
        return null;
    }
}
