package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import net.minecraft.nbt.NumericTag;

@LuaClass(wraps = NumericTag.class)
public class NumericTagWrap {
    @LuaMethod
    public static double get(NumericTag tag) {
        return tag.getAsDouble();
    }

    @LuaMetatable
    public static String __tostring(NumericTag tag) {
        return Double.toString(tag.getAsDouble());
    }
}
