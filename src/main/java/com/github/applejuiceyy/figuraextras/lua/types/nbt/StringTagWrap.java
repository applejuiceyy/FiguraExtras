package com.github.applejuiceyy.figuraextras.lua.types.nbt;

import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import net.minecraft.nbt.StringTag;

@LuaClass(wraps = StringTag.class)
public class StringTagWrap {
    @LuaMetatable
    public static String __tostring(StringTag tag) {
        return tag.getAsString();
    }
}
