package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.luabridge.annotation.IsIndex;
import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.Tag;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

@LuaClass(wraps = CollectionTag.class)
public class CollectionTagWrap {
    @LuaMethod
    public static boolean set(CollectionTag<?> tag, @IsIndex int idx, Tag child) {
        return tag.setTag(idx, child);
    }

    @LuaMethod
    public static boolean add(CollectionTag<?> tag, @IsIndex int idx, Tag child) {
        return tag.addTag(idx, child);
    }

    @LuaMethod
    public static void remove(CollectionTag<?> tag, @IsIndex int idx) {
        tag.remove(idx);
    }

    @LuaMethod
    public static Tag get(CollectionTag<?> tag, @IsIndex int idx) {
        return tag.get(idx);
    }

    @LuaMethod
    public static byte childType(CollectionTag<?> tag) {
        return tag.getElementType();
    }

    @LuaMethod
    public static int length(CollectionTag<?> tag) {
        return tag.size();
    }

    @LuaMethod
    public static String readString(CollectionTag<?> tag) {
        byte t = tag.getElementType();
        if (t != Tag.TAG_BYTE) {
            throw new LuaError("containing type of list must be bytes");
        }
        byte[] bytes = new byte[tag.size()];

        for (int i = 0, tagSize = tag.size(); i < tagSize; i++) {
            Tag tag1 = tag.get(i);
            bytes[i] = ((ByteTag) tag1).getAsByte();
        }

        return new String(bytes);
    }

    @LuaMetatable
    public static Tag __index(CollectionTag<?> tag, @IsIndex int key) {
        return tag.get(key);
    }

    @LuaMetatable
    public static void __newindex(CollectionTag<?> tag, @IsIndex int key, Tag child) {
        tag.setTag(key, child);
    }

    @LuaMetatable
    public static void __newindex(CollectionTag<?> tag, @IsIndex int key, LuaValue child) {
        __newindex(tag, key, TagWrap.convertLuaToNbt(child));
    }
}
