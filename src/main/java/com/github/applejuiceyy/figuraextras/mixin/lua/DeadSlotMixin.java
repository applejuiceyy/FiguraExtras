package com.github.applejuiceyy.figuraextras.mixin.lua;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mixin(targets = "org.luaj.vm2.LuaTable$DeadSlot", remap = false)
public class DeadSlotMixin {
    @Unique
    private final static Method removeMethod;
    @Unique
    private final static Field nextField;

    static {
        try {
            removeMethod = Class.forName("org.luaj.vm2.LuaTable$Slot")
                    .getMethod("remove", Class.forName("org.luaj.vm2.LuaTable$StrongSlot"));
            nextField = Class.forName("org.luaj.vm2.LuaTable$DeadSlot")
                    .getDeclaredField("next");
        } catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"InvalidInjectorMethodSignature", "rawtypes"})
    // it's drunk again (and it's also a bunch of private classes)
    @Inject(method = "remove", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    void a(@Coerce Object target, CallbackInfoReturnable cir) throws InvocationTargetException, IllegalAccessException {
        //noinspection unchecked
        Object obj = nextField.get(this);
        cir.setReturnValue(obj == null ? null : removeMethod.invoke(obj, target));
    }
}
