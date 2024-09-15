package com.github.applejuiceyy.luabridge.limiting;

public interface InstructionLimiter {
    void restrict(int instructionCount);

    void free();
}
