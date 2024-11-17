package com.github.applejuiceyy.figuraextras.tech.captures;

import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.lang.reflect.Method;
import java.util.Optional;

public interface Hook {
    default void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.CallType type, String possibleName) {
    }

    default void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, Object returns, LuaDuck.ReturnType type) {
    }

    default void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
    }

    default void endError(Object err) {
        end();
    }

    default void end() {
    }

    default void startEvent(Discernible discernible) {
    }

    default void startInit(String name) {
    }

    default void marker(String name) {
    }

    default void region(String regionName) {
    }

    default void intoJavaFunction(Varargs args, Method val$method, LuaDuck.CallType type) {
    }

    default void outOfJavaFunction(Varargs args, Method val$method, Object result, LuaDuck.ReturnType type) {
    }

    default void intoPCall() {
    }

    default void outOfPCall() {
    }

    interface Discernible {
        /**
         * basically an easier version of instanceof
         * @param cls The type to instanceof into
         * @return The cast type
         */
        default <T extends Discernible> Optional<T> getAs(Class<T> cls) {
            //noinspection unchecked
            return cls.isAssignableFrom(this.getClass()) ? Optional.of((T) this) : Optional.empty();
        }
    }
}
