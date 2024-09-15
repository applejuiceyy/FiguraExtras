package com.github.applejuiceyy.luabridge.asm.plugins;


import com.github.applejuiceyy.luabridge.asm.ArgumentConverterContext;
import com.github.applejuiceyy.luabridge.asm.CTMethodArgumentConverterHandler;
import com.github.applejuiceyy.luabridge.asm.Consts;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.nio.file.Path;

import static org.objectweb.asm.Opcodes.*;

public class PathArgumentConverterPlugin implements CTMethodArgumentConverterHandler {
    // LuaValue -> String
    @Override
    public void convert(Class<?> target, Annotation[] settings, ArgumentConverterContext context) {
        // LuaValue -> LuaValue LuaValue
        context.visitor.visitInsn(DUP);
        // LuaValue -> LuaValue Z
        context.visitor.visitMethodInsn(
                INVOKEVIRTUAL,
                Consts.LUA_VALUE.getInternalName(),
                "isstring",
                "()Z",
                false
        );
        // LuaValue Z -> LuaValue
        context.visitor.visitJumpInsn(IFEQ, context.dismiss);
        // LuaValue -> String
        context.visitor.visitMethodInsn(
                INVOKEVIRTUAL,
                Consts.LUA_VALUE.getInternalName(),
                "tojstring",
                "()Ljava/lang/String;",
                false
        );
        // String -> String 0
        context.visitor.visitInsn(ICONST_0);
        // String 0 -> String String[]
        context.visitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));
        // String String[] -> Path
        context.visitor.visitMethodInsn(
                INVOKESTATIC,
                Type.getInternalName(Path.class),
                "of",
                Type.getMethodDescriptor(Type.getType(Path.class), Type.getType(String.class), Type.getType(String[].class)),
                false
        );
    }

    @Override
    public Class<?>[] getHandlingTypes() {
        return new Class[]{Path.class};
    }
}
