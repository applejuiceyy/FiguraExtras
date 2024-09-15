package com.github.applejuiceyy.luabridge;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public interface OverloadedMethodHook extends Converter {
    void enter(Varargs args);

    void enterResolved(String name);

    void exit(Varargs ret);

    void error(Throwable th);

    LuaValue wrap(Object object);
}
