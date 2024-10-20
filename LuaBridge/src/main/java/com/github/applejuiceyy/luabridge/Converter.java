package com.github.applejuiceyy.luabridge;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public interface Converter {
    default Object toJava(LuaValue value) {
        return toJava(value, false);
    }

    Object toJava(LuaValue value, boolean isIndex);

    default LuaValue toLua(Object object) {
        return toLua(object, false);
    }

    LuaValue toLua(Object object, boolean isIndex);

    default Varargs toLuaVarargs(Object... object) {
        return toLuaVarargs(false, object);
    }

    Varargs toLuaVarargs(boolean isIndex, Object... object);
}
