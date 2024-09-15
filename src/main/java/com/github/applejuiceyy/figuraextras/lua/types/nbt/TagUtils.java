package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import net.minecraft.nbt.*;

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
}
