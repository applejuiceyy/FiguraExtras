package com.github.applejuiceyy.figuraextras.vscode.dsp;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.mixin.figura.LuaRuntimeAccessor;
import com.mojang.datafixers.util.Either;
import org.apache.commons.lang3.RegExUtils;
import org.eclipse.lsp4j.RegularExpressionsCapabilities;
import org.eclipse.lsp4j.debug.*;
import org.luaj.vm2.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BreakpointState {

    final Breakpoint breakpoint;
    private final SourceBreakpoint source;
    private final DebugProtocolServer owner;
    private int hitTimes = 0;

    BreakpointState(DebugProtocolServer owner, Breakpoint breakpoint, SourceBreakpoint source) {
        this.breakpoint = breakpoint;
        this.source = source;
        this.owner = owner;
    }

    public boolean process() {
        List<Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame>> frameList = owner.stackTrace.frameList;
        StackTraceTracker.LuaFrame frame = frameList.get(frameList.size() - 1).right().orElseThrow();
        if (source.getCondition() != null) {
            Varargs returnValue = composeAndCall(source.getCondition(), frame);
            if (!returnValue.arg1().toboolean()) {
                return false;
            }
        }
        if (source.getHitCondition() != null) {
            hitTimes++;
            Varargs returnValue = composeAndCall(source.getCondition(), frame);
            LuaValue number = returnValue.arg1().tonumber();
            if (number.isnil()) {
                return false;
            }
            int i = number.checkint();
            if (i >= hitTimes) {
                hitTimes = 0;
            } else {
                return false;
            }
        }
        String message = source.getLogMessage();
        if (message == null) {
            return true;
        }
        OutputEventArguments outputEventArguments = new OutputEventArguments();
        outputEventArguments.setCategory(OutputEventArgumentsCategory.STDOUT);
        String output = withInterpolation(message, frame);
        outputEventArguments.setOutput(output);
        outputEventArguments.setLine(breakpoint.getLine());
        outputEventArguments.setSource(breakpoint.getSource());
        owner.client.output(outputEventArguments);
        FiguraExtras.sendBrandedMessage("Breakpoint", output);
        return false;
    }

    public LuaValue composeAndCall(String toRun, StackTraceTracker.LuaFrame frame) {
        StringBuilder builder = new StringBuilder();
        Prototype p = frame.closure.p;
        LuaTable args = new LuaTable();

        builder.append("local function runner(");
        boolean first = true;
        for (int i = 0; i < p.upvalues.length; i++) {
            String name = p.upvalues[i].name.tojstring();
            LuaValue value = frame.closure.upValues[i].getValue();
            if (!first) builder.append(", ");
            first = false;
            builder.append(name);
            args.set(args.len().checkint() + 1, value);
        }
        List<String> activeVariables = new ArrayList<>();

        for (LocVars var : p.locvars) {
            if (var.startpc <= frame.pc && var.endpc >= frame.pc) {
                activeVariables.add(var.varname.tojstring());
            }
        }

        if (p.numparams > 0) {
            activeVariables.subList(0, p.numparams).clear();
        }

        for (int i = 0; i < activeVariables.size(); i++) {
            String name = activeVariables.get(i);
            if (!name.startsWith("(")) {
                LuaValue value = frame.stack[i + p.numparams];
                if (!first) builder.append(", ");
                first = false;
                builder.append(name);
                args.set(args.len().checkint() + 1, value);
            }
        }
        builder.append(")\nreturn ");
        builder.append(toRun);
        builder.append(";\nend");
        builder.append(" return runner(...)");
        try {
            return ((LuaRuntimeAccessor) owner.getCurrentAvatar().luaRuntime).getUserGlobals()
                    .load(builder.toString()).invoke(args.unpack()).arg1();
        } catch (LuaError error) {
            OutputEventArguments outputEventArguments = new OutputEventArguments();
            outputEventArguments.setCategory(OutputEventArgumentsCategory.IMPORTANT);
            outputEventArguments.setOutput("A breakpoint failed whilst executing conditions");
            outputEventArguments.setLine(breakpoint.getLine());
            outputEventArguments.setSource(breakpoint.getSource());
            owner.client.output(outputEventArguments);
            return LuaValue.NIL;
        }
    }

    String withInterpolation(String in, StackTraceTracker.LuaFrame frame) {
        Matcher matcher = Pattern.compile("\\{([^}]+)}").matcher(in);
        return matcher.replaceAll(result -> composeAndCall(result.group(1), frame).arg1().toString());
    }
}
