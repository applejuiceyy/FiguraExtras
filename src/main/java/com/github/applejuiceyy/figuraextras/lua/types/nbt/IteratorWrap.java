package com.github.applejuiceyy.figuraextras.lua.types.nbt;


import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;

import java.util.Iterator;

@LuaClass
public class IteratorWrap {
    @LuaMethod
    public boolean finished(Iterator<?> iterator) {
        return !iterator.hasNext();
    }

    @LuaMethod
    public Object next(Iterator<?> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @LuaMetatable
    public Object __call(Iterator<?> iterator, Object a, Object b) {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}
