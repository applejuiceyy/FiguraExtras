package com.github.applejuiceyy.luabridge.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ArgumentResolverContext {
    public final ClassVisitor classVisitor;
    public final ASMDispatchGenerator owner;
    public final MethodVisitor visitor;

    public ArgumentResolverContext(MethodVisitor visitor, ClassVisitor classVisitor, ASMDispatchGenerator owner) {
        this.visitor = visitor;
        this.classVisitor = classVisitor;
        this.owner = owner;
    }
}
