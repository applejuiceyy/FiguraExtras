package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.luabridge.annotation.IsNullable;
import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import org.luaj.vm2.LuaValue;

import java.util.Iterator;

@LuaClass(wraps = Iterator.class)
public class IteratorWrap {
    @LuaMethod
    public static boolean finished(Iterator<?> iterator) {
        return !iterator.hasNext();
    }

    @LuaMethod
    public static Object next(Iterator<?> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @LuaMetatable
    @IsNullable
    public static Object __call(Iterator<?> iterator, LuaValue jargonA, LuaValue jargonB) {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}
