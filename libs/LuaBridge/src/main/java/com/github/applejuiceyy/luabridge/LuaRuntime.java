package com.github.applejuiceyy.luabridge;

import com.github.applejuiceyy.luabridge.limiting.InstructionLimiter;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class LuaRuntime<BRIDGE extends Bridge> {

    public final BRIDGE bridge;
    private final Globals globals;
    private final ReentrantLock reentrantLock;
    private final InstructionLimiter limiter;
    private final LuaValue packageLib;
    private final LuaValue require;

    private boolean running = false;

    public LuaRuntime(BRIDGE bridge, Function<Globals, InstructionLimiter> limiter) {
        bridge.setOwner(this);
        this.bridge = bridge;
        globals = new Globals();
        reentrantLock = new ReentrantLock();
        LuaC.install(globals);

        globals.load(new PackageLib());

        packageLib = globals.get("package");
        packageLib.set("config", LuaValue.NIL);
        packageLib.set("path", LuaValue.NIL);
        packageLib.set("searchpath", LuaValue.NIL);
        LuaValue preload = packageLib.get("searchers").get(1);
        packageLib.set("searchers", new LuaTable(preload));

        require = globals.get("require");

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
    }

    public void addSearcher(LuaFileSearcher searcher) {
        if (isRunning()) {
            LuaValue searchers = packageLib.get("searchers");
            searchers.set(searchers.len().checkint() + 1, new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    try {
                        return searcher.fetch(args.checkjstring(1));
                    } catch (SearchException e) {
                        return LuaValue.valueOf(e.getMessage());
                    }
                }
            });
            return;
        }
        run(() -> {
            addSearcher(searcher);
            return LuaValue.NIL;
        }, 400);
    }

    protected abstract void printImplementation(Varargs args);

    public LuaValue requireFile(String path) {
        return require.call(path);
    }

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
        } catch (LuaError err) {
            throw err;
        } catch (Throwable throwable) {
            throw new LuaError("Catastrophic error: " + throwable);
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

    public interface LuaFileSearcher {
        LuaValue fetch(String name) throws SearchException;
    }

    public static class SearchException extends Exception {
        public SearchException(String message) {
            super(message);
        }
    }
}
