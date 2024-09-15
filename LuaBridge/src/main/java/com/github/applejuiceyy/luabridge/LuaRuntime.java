package com.github.applejuiceyy.luabridge;

import com.github.applejuiceyy.luabridge.limiting.InstructionLimiter;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class LuaRuntime<BRIDGE extends Bridge> {

    public final BRIDGE bridge;
    private final Globals globals;
    private final ReentrantLock reentrantLock;
    private final InstructionLimiter limiter;
    private final HashMap<String, LuaValue> requireCache = new HashMap<>();

    private boolean running = false;

    public LuaRuntime(BRIDGE bridge, Function<Globals, InstructionLimiter> limiter) {
        bridge.setOwner(this);
        this.bridge = bridge;
        globals = new Globals();
        reentrantLock = new ReentrantLock();
        LuaC.install(globals);

        globals.load(new JseBaseLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new JseStringLib());
        globals.load(new JseMathLib());
        globals.load(new DebugLib());

        this.limiter = limiter.apply(globals);

        globals.set("debug", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("collectgarbage", LuaValue.NIL);

        globals.set("print", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                printImplementation(args);
                return NIL;
            }
        });
        globals.set("require", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return requireFile(arg.checkjstring());
            }
        });
    }

    protected abstract void printImplementation(Varargs args);

    public LuaValue requireFile(String path) {
        path = sanitizeRequirePath(path);
        if (!requireCache.containsKey(path)) {
            requireCache.put(path, requireImplementation(path));
        }
        return requireCache.get(path);
    }

    protected abstract String sanitizeRequirePath(String path);

    protected abstract LuaValue requireImplementation(String path);

    public Object get(String var) {
        return get(var, false);
    }

    public Object get(String var, boolean isIndex) {
        return bridge.toJava(globals.get(var), isIndex);
    }

    public void set(String name, Object var) {
        set(name, var, false);
    }

    public void set(String name, Object var, boolean isIndex) {
        globals.set(name, bridge.toLua(var, isIndex));
    }

    public Globals getGlobals() {
        return globals;
    }

    public boolean isRunning() {
        return running;
    }

    public Varargs run(LuaValue toRun, int instructionCount, Object... args) {
        return withPolicy(instructionCount, () -> run(toRun, args));
    }

    private Varargs withPolicy(int instructionCount, Supplier<Varargs> inner) {
        reentrantLock.lock();
        try {
            limiter.restrict(instructionCount);
            try {
                return inner.get();
            } finally {
                limiter.free();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    public Varargs run(LuaValue toRun, Object... args) {
        return run(() -> toRun.invoke(bridge.toLua(args, false)));
    }

    public Varargs run(Supplier<Varargs> toRun) {
        reentrantLock.lock();
        if (running) {
            reentrantLock.unlock();
            throw new RuntimeException("Reentry attempt");
        }
        running = true;
        Varargs varargs;
        try {
            varargs = toRun.get();
        } catch (StackOverflowError err) {
            throw new LuaError("Stack Overflow");
        } finally {
            running = false;
            reentrantLock.unlock();
        }
        return varargs;
    }

    public Varargs run(LuaValue toRun, int instructionCount, Varargs args) {
        return withPolicy(instructionCount, () -> run(toRun, args));
    }

    public Varargs run(LuaValue toRun, Varargs args) {
        return run(() -> toRun.invoke(args));
    }

    public Varargs run(Supplier<Varargs> toRun, int instructionCount) {
        return withPolicy(instructionCount, () -> run(toRun));
    }

    public LuaValue load(String chunkName, String content) {
        return globals.load(content, chunkName);
    }
}
