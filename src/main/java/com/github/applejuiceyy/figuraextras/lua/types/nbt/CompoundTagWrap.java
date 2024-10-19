package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.luabridge.Converter;
import com.github.applejuiceyy.luabridge.annotation.IsNullable;
import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.include.com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.Set;

@LuaClass(wraps = CompoundTag.class)
public class CompoundTagWrap {
    @LuaMethod
    public static void set(CompoundTag tag, String index, Tag child) {
        tag.put(index, child);
    }

    @LuaMethod
    public static Tag get(CompoundTag tag, String index) {
        return tag.get(index);
    }

    @LuaMethod
    public static void remove(CompoundTag tag, String index) {
        tag.remove(index);
    }

    @LuaMethod
    public static int length(CompoundTag tag) {
        return tag.size();
    }

    @LuaMethod
    public static Iterator<Varargs> entries(Converter converter, CompoundTag tag) {
        Set<String> keys = tag.getAllKeys();
        Varargs[] tuples = new Varargs[keys.size()];
        int i = 0;
        for (String key : keys) {
            tuples[i++] = LuaValue.varargsOf(converter.toLua(key), converter.toLua(tag.get(key)));
        }
        return Iterators.forArray(tuples);
    }

    @LuaMetatable
    public static Tag __index(CompoundTag tag, String index) {
        return tag.get(index);
    }

    @LuaMetatable
    public static void __newindex(CompoundTag tag, String key, LuaValue child) {
        __newindex(tag, key, TagWrap.convertLuaToNbt(child));
    }

    @LuaMetatable
    public static void __newindex(CompoundTag tag, String key, @IsNullable Tag child) {
        if (child == null) {
            tag.remove(key);
        } else {
            tag.put(key, child);
        }
    }
}
