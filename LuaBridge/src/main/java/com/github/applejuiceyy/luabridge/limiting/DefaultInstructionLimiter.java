package com.github.applejuiceyy.luabridge.limiting;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class DefaultInstructionLimiter implements InstructionLimiter {

    private final LuaValue setHook;
    private int instructionCount;

    public DefaultInstructionLimiter(Globals globals) {
        setHook = globals.get("debug").get("sethook");
    }

    @Override
    public void restrict(int instructionCount) {
        setHook.call(new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                if (DefaultInstructionLimiter.this.instructionCount-- == 0) {
                    throw new LuaError("Exceeded instruction limit");
                }
                return null;
            }
        }, LuaValue.valueOf("c"), LuaValue.valueOf(1));
        this.instructionCount = instructionCount;
    }

    @Override
    public void free() {
        setHook.call();
        instructionCount = -1;
    }
}
