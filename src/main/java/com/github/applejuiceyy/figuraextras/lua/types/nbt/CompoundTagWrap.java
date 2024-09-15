package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.luabridge.Converter;
import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.Iterator;

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
    public static int length(CompoundTag tag) {
        return tag.size();
    }

    @LuaMethod
    public static Iterator<String> keys(CompoundTag tag) {
        return tag.getAllKeys().iterator();
    }

    @LuaMethod
    public static Iterator<Tag> values(CompoundTag tag) {
        Iterator<String> allKeys = tag.getAllKeys().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return allKeys.hasNext();
            }

            @Override
            public Tag next() {
                return tag.get(allKeys.next());
            }
        };
    }

    @LuaMethod
    public static Iterator<Varargs> entries(Converter converter, CompoundTag tag) {
        Iterator<String> allKeys = tag.getAllKeys().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return allKeys.hasNext();
            }

            @Override
            public Varargs next() {
                String next = allKeys.next();
                return LuaValue.varargsOf(converter.toLua(next), converter.toLua(tag.get(next), false));
            }
        };
    }

    @LuaMetatable
    public static Tag __index(CompoundTag tag, String index) {
        return tag.get(index);
    }

    @LuaMetatable
    public static void __newindex(CompoundTag tag, String key, Tag child) {
        tag.put(key, child);
    }
}
