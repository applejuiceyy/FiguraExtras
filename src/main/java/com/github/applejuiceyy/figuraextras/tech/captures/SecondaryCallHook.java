package com.github.applejuiceyy.figuraextras.tech.captures;

import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public interface SecondaryCallHook {
    void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.CallType type);

    void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.ReturnType type);

    void lineAdvanced();

    void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack);

    void end();

    void marker(String name);

    void region(String regionName);
}
