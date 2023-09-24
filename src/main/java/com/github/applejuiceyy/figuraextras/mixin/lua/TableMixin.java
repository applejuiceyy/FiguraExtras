package com.github.applejuiceyy.figuraextras.mixin.lua;

import org.luaj.vm2.LuaTable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LuaTable.class, remap = false)
public class TableMixin {
}
