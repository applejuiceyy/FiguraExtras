package com.github.applejuiceyy.figuraextras.ipc.dsp;


import com.github.applejuiceyy.figuraextras.util.Util;
import net.minecraft.util.Tuple;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablePresentationHint;
import org.eclipse.lsp4j.debug.VariablePresentationHintKind;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.math.matrix.FiguraMatrix;
import org.figuramc.figura.math.vector.FiguraVector;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO: find a way to merge this with tech.trees
public class VariableRenderer {
    private static HashMap<Class<Object>, Function<LuaValue, String>> interpreters = new HashMap<>();
    private static HashMap<Class<Object>, Function<LuaValue, List<Tuple<Variable, LuaValue>>>> expanders = new HashMap<>();

    public static <T extends LuaValue> String interpret(T luaValue) {
        if (interpreters.containsKey(luaValue.getClass())) {
            return interpreters.get(luaValue.getClass()).apply(luaValue);
        }

        if (luaValue.getmetatable() != null) {
            LuaValue func = luaValue.getmetatable().get("__debug_value");
            if (!func.isnil())
                return func.invoke().toString();
        }


        if (luaValue.isuserdata()) {
            Object o = luaValue.checkuserdata();

            if (o instanceof FiguraVector<?, ?> || o instanceof FiguraMatrix<?, ?>) {
                return o.toString().replace("\n", "");
            }
        }

        if (luaValue.isstring() && !(luaValue instanceof LuaNumber)) {
            return "\"" + luaValue + "\"";
        } else {
            return luaValue.toString();
        }
    }

    public static <T extends LuaValue> Supplier<List<Tuple<Variable, LuaValue>>> expand(Avatar avatar, T luaValue) {
        if (expanders.containsKey(luaValue.getClass())) {
            return () -> expanders.get(luaValue.getClass()).apply(luaValue);
        }

        LuaValue func;
        LuaValue iterable;
        if (luaValue.getmetatable() != null && !(func = luaValue.getmetatable().get("__debug_fields")).isnil()) {
            iterable = func.invoke().arg1();
        } else {
            if (luaValue.isuserdata()) {
                Object o = luaValue.checkuserdata();

                if (o instanceof FiguraVector<?, ?> vec) {
                    return () -> {
                        double[] values = vec.unpack();

                        ArrayList<Tuple<Variable, LuaValue>> list = new ArrayList<>();
                        for (int i = 0; i < values.length; i++) {
                            double value = values[i];

                            Variable variable = new Variable();
                            variable.setName("Position " + (i + 1));
                            list.add(new Tuple<>(variable, LuaValue.valueOf(value)));
                        }

                        return list;
                    };
                }

                if (o instanceof FiguraMatrix<?, ?> vec) {
                    return () -> {
                        int rows = vec.rows();

                        ArrayList<Tuple<Variable, LuaValue>> list = new ArrayList<>();

                        for (int i = 0; i < rows; i++) {
                            Variable variable = new Variable();
                            variable.setName("Row  " + (i + 1));
                            list.add(new Tuple<>(variable, avatar.luaRuntime.typeManager.javaToLua(vec.getRow(i + 1)).arg1()));
                        }

                        return list;
                    };
                }
            }


            iterable = luaValue;
        }

        if (iterable.istable()) {
            return () -> {
                List<Tuple<Variable, LuaValue>> variables = new ArrayList<>();

                for (Tuple<LuaValue, LuaValue> variable : Util.iterateLua(iterable)) {
                    Variable t = new Variable();
                    t.setName(variable.getA().tojstring());
                    variables.add(new Tuple<>(t, variable.getB()));
                }

                if (iterable.getmetatable() != null) {
                    Variable t = new Variable();
                    t.setName("@metatable");
                    VariablePresentationHint variablePresentationHint = new VariablePresentationHint();
                    t.setPresentationHint(variablePresentationHint);
                    variablePresentationHint.setKind(VariablePresentationHintKind.VIRTUAL);
                    variables.add(new Tuple<>(t, iterable.getmetatable()));
                }

                return variables;
            };
        }

        return null;
    }

    public static String getType(Avatar avatar, LuaValue value) {
        if (value.getmetatable() != null) {
            LuaValue func = value.getmetatable().get("__debug_type");
            if (!func.isnil()) {
                return func.invoke().toString();
            }
        }

        if (value.isuserdata()) {
            Class<?> cls = value.checkuserdata().getClass();
            return avatar.luaRuntime.typeManager.getTypeName(cls);
        } else {
            return value.typename();
        }
    }

    public static <T> void registerInterpreter(Class<? extends T> cls, Function<T, String> interpreter) {
        //noinspection unchecked
        interpreters.put((Class<Object>) cls, (Function<LuaValue, String>) interpreter);
    }

    public static <T> void registerExpander(Class<? extends T> cls, Function<T, List<Tuple<Variable, LuaValue>>> interpreter) {
        //noinspection unchecked
        expanders.put((Class<Object>) cls, (Function<LuaValue, List<Tuple<Variable, LuaValue>>>) interpreter);
    }
}
