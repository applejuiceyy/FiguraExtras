package com.github.applejuiceyy.luabridge.asm;


public interface CTMethodArgumentResolverHandler {
    void resolve(Class<?> target, ASMDispatchGenerator.ParamNode params, ArgumentResolverContext context);

    Class<?>[] getHandlingTypes();
}
