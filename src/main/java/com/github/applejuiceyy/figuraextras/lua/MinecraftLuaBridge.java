package com.github.applejuiceyy.figuraextras.lua;

import com.github.applejuiceyy.luabridge.Bridge;
import com.github.applejuiceyy.luabridge.ClassMap;
import com.github.applejuiceyy.luabridge.Collector;
import com.github.applejuiceyy.luabridge.DispatchGenerator;
import com.github.applejuiceyy.luabridge.annotation.LuaPrint;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.lua.LuaTypeManager;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.Consumer;

public class MinecraftLuaBridge extends Bridge {
    // TODO: native support for figura's stuff
    ClassMap<PrinterCallback> printers;
    LuaTypeManager o = null;

    public MinecraftLuaBridge(Set<Class<?>> types, ClassMap<Class<?>> wrapping, DispatchGenerator generator) {
        super(types, wrapping, generator);
        printers = new ClassMap<>();
    }

    public void setFiguraTypeManager(LuaTypeManager o) {
        this.o = o;
    }

    @Override
    protected void attachCollectors(Class<?> cls, Class<?> wrapping, Consumer<Collector<Method>> collectorConsumer) {
        super.attachCollectors(cls, wrapping, collectorConsumer);
        collectorConsumer.accept(new Collector<>() {
            PrinterCallback printerCallback;

            @Override
            public void collect(Method method) {
                if (method.getAnnotation(LuaPrint.class) != null) {
                    if ((cls != wrapping) && !Modifier.isStatic(method.getModifiers())) {
                        throw new IllegalStateException("A class wrapping another class can only have static methods");
                    }

                    MethodHandle handle;
                    try {
                        handle = MethodHandles.lookup().unreflect(method);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Does not have access to printing method");
                    }
                    MethodHandle finalHandle = handle;
                    printerCallback = (a, b) -> {
                        try {
                            return (Component) finalHandle.invokeExact(a, b);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
            }

            @Override
            public void end() {
                if (printerCallback != null) {
                    printers.add(wrapping, printerCallback);
                }
            }
        });
    }

    @Override
    public LuaValue wrap(Object val) {
        try {
            return super.wrap(val);
        } catch (LuaError err) {
            if (o != null) {
                try {
                    return o.javaToLua(val).arg1();
                } catch (RuntimeException ignored) {
                }
            }
            throw err;
        }
    }

    public Component getPrintableValue(Varargs args) {
        MutableComponent c = Component.empty();
        for (int i = 1; i <= args.narg(); i++) {
            c.append(getPrintableValue(args.arg(i)));
        }
        return c;
    }

    public Component getPrintableValue(LuaValue val) {
        if (val.isuserdata()) {
            Object obj = val.checkuserdata(Object.class);
            Class<?> type = obj.getClass();

            PrinterCallback first = printers.findFirst(type);
            if (first == null) {
                return Component.literal(type.getSimpleName()).withStyle(ChatFormatting.YELLOW);
            }
            return first.convert(obj, false);
        }

        if (val.istable())
            return Component.literal(val.tojstring()).withStyle(ChatFormatting.DARK_PURPLE);
        else if (val.isinttype())
            return Component.literal(val.checkjstring()).withStyle(ChatFormatting.BLUE);
        else if (val.isstring())
            return Component.literal(val.checkjstring());
        else if (val.isboolean())
            return Component.literal(val.checkboolean() ? "true" : "false").withStyle(ChatFormatting.BLUE);
        else if (val.isfunction())
            return Component.literal(val.toString()).withStyle(ChatFormatting.GREEN);
        else
            return Component.literal(val.toString());
    }

    public interface PrinterCallback {
        Component convert(Object object, boolean expand);
    }
}
