package com.github.applejuiceyy.luabridge.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ArgumentConverterContext extends ArgumentResolverContext {
    public final Label dismiss;

    public ArgumentConverterContext(Label dismiss, MethodVisitor visitor, ClassVisitor classVisitor, ASMDispatchGenerator owner) {
        super(visitor, classVisitor, owner);
        this.dismiss = dismiss;
    }
}
