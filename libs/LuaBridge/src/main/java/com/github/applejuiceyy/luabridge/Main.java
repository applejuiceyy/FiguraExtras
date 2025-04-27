package com.github.applejuiceyy.luabridge;

import com.github.applejuiceyy.luabridge.asm.ASMDispatchGenerator;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.Arrays;
import java.util.function.Function;

public class Main {
    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException {
        OverloadedMethod method = new OverloadedMethod("passant", Bridge.create().generator(ASMDispatchGenerator.create().build()).build());
        method.addOverload(Main.class.getMethod("e", int.class, Integer[].class));
        method.addOverload(Main.class.getMethod("e", int.class, String[].class));
        method.addOverload(Main.class.getMethod("e", int.class));
        Function<Varargs, Varargs> function = method.generateDispatch();
        function.apply(
                LuaValue.varargsOf(
                        new LuaValue[]{
                                LuaValue.valueOf(1),
                                LuaValue.valueOf(2),
                                LuaValue.valueOf(3),
                                LuaValue.valueOf(4),
                                LuaValue.valueOf(5)
                        }
                )
        );
        function.apply(LuaValue.varargsOf(new LuaValue[]{
                LuaValue.valueOf(1),
                LuaValue.valueOf(2),
                LuaValue.valueOf(3),
                LuaValue.valueOf(4),
                LuaValue.valueOf("")
        }));
    }

    public static void e(int o, Integer... tarmacs) {
        System.out.println(o);
        System.out.println(Arrays.toString(tarmacs));
    }

    public static void e(int o, String... tarmacs) {
        System.out.println(o);
        System.out.println(Arrays.toString(tarmacs));
    }

    public static void e(int o) {
        System.out.println(o);
    }
}
