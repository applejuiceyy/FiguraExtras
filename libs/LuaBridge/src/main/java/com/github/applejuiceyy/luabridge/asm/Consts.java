package com.github.applejuiceyy.luabridge.asm;

import com.github.applejuiceyy.luabridge.OverloadedMethod;
import com.github.applejuiceyy.luabridge.OverloadedMethodHook;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.Type;

public class Consts {
    public static final String PACKAGE_NAME = ASMDispatchGenerator.class.getPackageName().replace('.', '/');
    public static final Type SURROGATE_CLASS_NAME = Type.getType("L" + PACKAGE_NAME + "/process;");
    public static final Type OWNER_NAME = Type.getType(OverloadedMethod.class);
    public static final Type HOOK_NAME = Type.getType(OverloadedMethodHook.class);
    public static final Type LUA_ERROR = Type.getType(LuaError.class);
    public static final Type LUA_VALUE = Type.getType(LuaValue.class);
    public static final Type VARARGS = Type.getType(Varargs.class);
}
