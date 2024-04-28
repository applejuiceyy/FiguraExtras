package com.github.applejuiceyy.figuraextras.tech.captures;

import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.lang.reflect.Method;

public interface Hook {
    default void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.CallType type, String possibleName) {
    }

    ;

    default void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, Object returns, LuaDuck.ReturnType type) {
    }

    ;

    default void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
    }

    ;

    default void end() {
    }

    default void endError(Object err) {
        end();
    }

    ;

    default void startEvent(String runReason, Object toRun, Varargs val) {
    }

    ;

    default void startInit(String name) {
    }

    ;

    default void marker(String name) {
    }

    ;

    default void region(String regionName) {
    }

    ;

    default void intoJavaFunction(Varargs args, Method val$method, LuaDuck.CallType type) {
    }

    ;

    default void outOfJavaFunction(Varargs args, Method val$method, Object result, LuaDuck.ReturnType type) {
    }

    default void intoPCall() {
    }

    ;

    default void outOfPCall() {
    }

    ;
}
