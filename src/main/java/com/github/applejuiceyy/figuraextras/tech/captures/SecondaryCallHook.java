package com.github.applejuiceyy.figuraextras.tech.captures;

import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public interface SecondaryCallHook {
    void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack);

    void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack);

    void lineAdvanced();

    void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack);

    void end();

    void marker(String name);

    void region(String regionName);
}
