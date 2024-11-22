package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.nbt.*;
import net.minecraft.util.Tuple;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

@LuaClass
public class TagUtils {
    @LuaMethod
    public ShortTag newShortTag(short value) {
        return ShortTag.valueOf(value);
    }

    @LuaMethod
    public DoubleTag newDoubleTag(double value) {
        return DoubleTag.valueOf(value);
    }

    @LuaMethod
    public FloatTag newFloatTag(float value) {
        return FloatTag.valueOf(value);
    }

    @LuaMethod
    public ByteTag newByteTag(byte value) {
        return ByteTag.valueOf(value);
    }

    @LuaMethod
    public IntTag newIntTag(int value) {
        return IntTag.valueOf(value);
    }

    @LuaMethod
    public LongTag newLongTag(long value) {
        return LongTag.valueOf(value);
    }

    @LuaMethod
    public CompoundTag newCompoundTag() {
        return new CompoundTag();
    }

    @LuaMethod
    public StringTag newStringTag(String value) {
        return StringTag.valueOf(value);
    }

    @LuaMethod
    public LongArrayTag newLongArrayTag() {
        return new LongArrayTag(new long[0]);
    }

    @LuaMethod
    public ByteArrayTag newByteArrayTag() {
        return new ByteArrayTag(new byte[0]);
    }

    @LuaMethod
    public IntArrayTag newIntArrayTag() {
        return new IntArrayTag(new int[0]);
    }

    @LuaMethod
    public ListTag newArrayTag() {
        return new ListTag();
    }

    @LuaMethod
    public Tag readNbt(String nbt) {
        TagParser parser = new TagParser(new StringReader(nbt));
        try {
            return parser.readValue();
        } catch (CommandSyntaxException e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaMethod
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
