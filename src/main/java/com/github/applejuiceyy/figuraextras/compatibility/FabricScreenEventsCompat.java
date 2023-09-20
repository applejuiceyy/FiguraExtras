package com.github.applejuiceyy.figuraextras.compatibility;

import com.github.applejuiceyy.figuraextras.screen.ScreenContainer;
import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class FabricScreenEventsCompat {
    private final ScreenContainer screenContainer;
    private static final HashMap<String, Method> cache = new HashMap<>();

    public FabricScreenEventsCompat(ScreenContainer screenContainer) {
        this.screenContainer = screenContainer;
    }

    public void invokeFabric(String methodName, Object... args) {
        try {
            String interfaceName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
            Class<?> extensionsClass = Class.forName("net.fabricmc.fabric.impl.client.screen.ScreenExtensions");
            Object invoker = extensionsClass.getMethod("fabric_get" + interfaceName + "Event").invoke(screenContainer.getScreen());
            Object invoke = invoker.getClass().getMethod("invoker").invoke(invoker);
            Method meth = invoke.getClass().getDeclaredMethods()[0];
            meth.setAccessible(true);
            Object[] builtArgs = new Object[args.length + 1];
            builtArgs[0] = screenContainer.getScreen();
            System.arraycopy(args, 0, builtArgs, 1, args.length);
            meth.invoke(invoke, builtArgs);
        } catch (IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Unsafe.getUnsafe().throwException(e.getTargetException());
        }
    }
}
