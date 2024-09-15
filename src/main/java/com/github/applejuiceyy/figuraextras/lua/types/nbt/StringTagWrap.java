package com.github.applejuiceyy.figuraextras.lua.types.nbt;

import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import net.minecraft.nbt.StringTag;

@LuaClass(wraps = StringTag.class)
public class StringTagWrap {
    public static String get(StringTag tag) {
        return tag.getAsString();
    }
}
