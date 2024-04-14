package com.github.applejuiceyy.figuraextras.vscode.dsp;


import com.mojang.datafixers.util.Either;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.ArrayList;
import java.util.List;

public class StackTraceTracker {
    String kickstarter;
    boolean isActive = false;
    List<Either<JavaFrame, LuaFrame>> frameList = new ArrayList<>();

    static class JavaFrame {
        String javaFrame;

        Varargs arguments;
    }

    static class LuaFrame {
        public String possibleName;
        public int line;
        public int pc;
        LuaClosure closure;
        LuaValue[] stack;
    }
}
