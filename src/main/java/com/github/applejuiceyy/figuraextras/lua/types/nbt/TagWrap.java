package com.github.applejuiceyy.figuraextras.lua.types.nbt;

import com.github.applejuiceyy.figuraextras.ducks.TextComponentTagVisitorAccess;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import com.github.applejuiceyy.luabridge.annotation.LuaPrint;
import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;


@LuaClass(wraps = Tag.class)
public class TagWrap {
    @LuaMethod
    public static String getType(Tag tag) {
        return tag.getType().getName();
    }

    @LuaMethod
    public static byte getId(Tag tag) {
        return tag.getId();
    }

    @LuaMethod
    public static Tag copy(Tag tag) {
        return tag.copy();
    }

    @LuaPrint
    public static Component print(Tag tag, boolean expand) {
        TextComponentTagVisitor visitor = new TextComponentTagVisitor("    ", 0);
        ((TextComponentTagVisitorAccess) visitor).figuraExtrass$setElementLimit(10);
        return visitor.visit(tag);
    }

    static Tag convertLuaToNbt(LuaValue value) {

        if (value.isstring() && !value.isinttype()) {
            return StringTag.valueOf(value.tojstring());
        }
        if (value.isnumber()) {
            return FloatTag.valueOf(value.tofloat());
        }
        if (value.istable()) {
            CompoundTag compoundTag = new CompoundTag();
            // it is tempting to change it to a list
            // but pairs isn't forced to give organised entries because they are numbers
            IntObjectHashMap<Tag> cache = new IntObjectHashMap<>();

            for (Tuple<LuaValue, LuaValue> pair : Util.iterateLua(value)) {
                LuaValue key = pair.getA();

                Tag nbt = convertLuaToNbt(pair.getB());
                if (!key.isinttype()) {
                    cache = null;
                } else if (cache != null) {
                    cache.put(key.toint(), nbt);
                }

                String tojstringed = key.tojstring();

                compoundTag.put(tojstringed, nbt);
            }
            if (cache != null) {
                ListTag listTag = new ListTag();

                for (Tuple<LuaValue, LuaValue> pair : Util.iterateLua(value, LuaValue::inext)) {
                    LuaValue key = pair.getA();

                    if (key.isinttype()) {
                        listTag.add(cache.get(key.toint()));
                    }
                }

                if (!listTag.isEmpty()) {
                    return listTag;
                }
            }
            return compoundTag;
        }
        throw new LuaError("Cannot convert " + value + " to an nbt analogue");
    }
}
