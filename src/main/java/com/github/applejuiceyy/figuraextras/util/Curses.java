package com.github.applejuiceyy.figuraextras.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Curses {
    public static MethodHandles.Lookup ALL;
    public static Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            theUnsafe.setAccessible(false);

            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            ALL = (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object callSuper(Method method, Object self, Object[] args) {
        Object[] objects;
        if (args == null) {
            objects = new Object[]{self};
        } else {
            objects = new Object[args.length + 1];
            objects[0] = self;
            System.arraycopy(args, 0, objects, 1, args.length);
        }
        try {
            return ALL.unreflectSpecial(method, self.getClass()).invokeWithArguments(objects);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
