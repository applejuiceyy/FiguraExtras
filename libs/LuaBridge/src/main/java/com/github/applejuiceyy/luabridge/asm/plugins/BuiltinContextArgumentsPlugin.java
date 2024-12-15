package com.github.applejuiceyy.luabridge.asm.plugins;

import com.github.applejuiceyy.luabridge.Bridge;
import com.github.applejuiceyy.luabridge.Converter;
import com.github.applejuiceyy.luabridge.LuaRuntime;
import com.github.applejuiceyy.luabridge.asm.ASMDispatchGenerator;
import com.github.applejuiceyy.luabridge.asm.ArgumentResolverContext;
import com.github.applejuiceyy.luabridge.asm.CTMethodArgumentResolverHandler;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;

public class BuiltinContextArgumentsPlugin implements CTMethodArgumentResolverHandler {
    // -> Converter
    @Override
    public void resolve(Class<?> target, ASMDispatchGenerator.ParamNode params, ArgumentResolverContext context) {
        if (target == LuaRuntime.class) {
            // -> LuaRuntime
            context.visitor.visitVarInsn(ALOAD, 1);
            return;
        }
        // -> Bridge
        context.visitor.visitVarInsn(ALOAD, 2);
        if (target == Converter.class) {
            // Bridge -> Converter
            context.visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Converter.class));
        }
    }

    @Override
    public Class<?>[] getHandlingTypes() {
        return new Class<?>[]{Converter.class, LuaRuntime.class, Bridge.class};
    }
}
