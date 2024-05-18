package com.github.applejuiceyy.figuraextras.ipc.dsp;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.lua.LuaRuntimeAccessor;
import com.mojang.datafixers.util.Either;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory;
import org.luaj.vm2.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaExecutor {
    private final DebugProtocolServer owner;

    LuaExecutor(DebugProtocolServer owner) {
        this.owner = owner;
    }

    public LuaValue composeAndCall(String toRun, Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame> frame, Function<OutputEventArguments, LuaValue> onError) {
        if (frame.right().isPresent()) {
            StackTraceTracker.LuaFrame luaFrame = frame.right().get();
            return composeAndCall(toRun, luaFrame, onError);
        } else if (frame.left().isPresent()) {
            return composeAndCall(toRun, new HashMap<>(), onError);
        }
        throw new IllegalStateException();
    }

    public LuaValue composeAndCall(String toRun, StackTraceTracker.LuaFrame frame, Function<OutputEventArguments, LuaValue> onError) {
        HashMap<String, LuaValue> arguments = new HashMap<>();
        Prototype p = frame.closure.p;

        for (int i = 0; i < p.upvalues.length; i++) {
            String name = p.upvalues[i].name.tojstring();
            LuaValue value = frame.closure.upValues[i].getValue();
            arguments.put(name, value);
        }

        int i = 0;
        for (LocVars var : p.locvars) {
            if (var.startpc <= frame.pc && var.endpc >= frame.pc) {
                if (!var.varname.tojstring().startsWith("(")) {
                    arguments.put(var.varname.tojstring(), frame.stack[i]);
                }
                i++;
            }
        }

        return composeAndCall(toRun, arguments, args -> {
            args.setLine(frame.line);
            args.setSource(owner.sourcer.toSource(frame.closure.p));
            return onError.apply(args);
        });
    }

    public LuaValue composeAndCall(String toRun, HashMap<String, LuaValue> vars, Function<OutputEventArguments, LuaValue> onError) {
        Globals globals = ((LuaRuntimeAccessor) owner.getCurrentAvatar().luaRuntime).getUserGlobals();
        if (!vars.containsKey("config")) {
            LuaTable config = new LuaTable() {
                @Override
                public LuaValue rawget(LuaValue key) {
                    if (key.eq(LuaValue.valueOf("sethook")).toboolean()) {
                        return LuaValue.FALSE;
                    }
                    return super.rawget(key);
                }
            };
            LuaTable configMeta = new LuaTable();
            configMeta.set("__metatable", LuaValue.FALSE);
            configMeta.set("__index", ((GlobalsAccess) globals).figuraExtrass$getOffTheShelfDebugLib());
            vars.put("config", config);
        }

        StringBuilder builder = new StringBuilder();
        LuaTable args = new LuaTable();

        builder.append("local function runner(");
        boolean first = true;
        for (Map.Entry<String, LuaValue> entry : vars.entrySet()) {
            if (!first) builder.append(", ");
            first = false;
            builder.append(entry.getKey());
            args.set(args.len().checkint() + 1, entry.getValue());
        }

        builder.append(")\nreturn ");
        builder.append(toRun);
        builder.append(";\nend");
        builder.append(" return runner(...)");
        boolean inhook = globals.running.state.inhook;
        globals.running.state.inhook = true;
        try {
            return globals
                    .load(builder.toString()).invoke(args.unpack()).arg1();
        } catch (LuaError error) {
            OutputEventArguments outputEventArguments = new OutputEventArguments();
            outputEventArguments.setCategory(OutputEventArgumentsCategory.IMPORTANT);
            LuaValue value = onError.apply(outputEventArguments);
            outputEventArguments.setOutput(outputEventArguments.getOutput() + ":\n\n" + error.getMessage());
            owner.client.output(outputEventArguments);
            return value;
        } finally {
            globals.running.state.inhook = inhook;
        }
    }

    String withInterpolation(String in, StackTraceTracker.LuaFrame frame, Function<OutputEventArguments, LuaValue> onError) {
        return withInterpolation(Pattern.compile("\\{([^}]+)}").matcher(in), frame, onError);
    }

    String withInterpolation(Matcher matcher, StackTraceTracker.LuaFrame frame, Function<OutputEventArguments, LuaValue> onError) {
        return matcher.replaceAll(result -> composeAndCall(result.group(1), frame, onError).arg1().toString());
    }
}
