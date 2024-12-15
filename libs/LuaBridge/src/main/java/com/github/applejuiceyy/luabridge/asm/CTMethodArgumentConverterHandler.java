package com.github.applejuiceyy.luabridge.asm;

import java.lang.annotation.Annotation;

public interface CTMethodArgumentConverterHandler {
    void convert(Class<?> target, Annotation[] settings, ArgumentConverterContext context);

    Class<?>[] getHandlingTypes();
}
