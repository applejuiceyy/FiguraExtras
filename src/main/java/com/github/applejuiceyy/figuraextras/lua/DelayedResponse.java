package com.github.applejuiceyy.figuraextras.lua;

import org.figuramc.figura.lua.LuaWhitelist;
import org.luaj.vm2.LuaValue;

import java.util.concurrent.CompletableFuture;

@LuaWhitelist
public class DelayedResponse extends CompletableFuture<LuaValue> {
    public DelayedResponse() {
        super();
    }

    @LuaWhitelist
    public boolean complete(LuaValue value) {
        return super.complete(value);
    }

    @LuaWhitelist
    public boolean isDone() {
        return super.isDone();
    }
}
