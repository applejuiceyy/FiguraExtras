package com.github.applejuiceyy.figuraextras.vscode.dsp;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.mojang.datafixers.util.Either;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.List;

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
            Varargs returnValue = owner.executor.composeAndCall(source.getCondition(), frame, args -> {
                args.setOutput("A breakpoint failed whilst executing condition");
                args.setLine(breakpoint.getLine());
                args.setSource(breakpoint.getSource());
                return LuaValue.TRUE;
            });
            if (!returnValue.arg1().toboolean()) {
                return false;
            }
        }
        if (source.getHitCondition() != null) {
            hitTimes++;
            Varargs returnValue = owner.executor.composeAndCall(source.getCondition(), frame, args -> {
                args.setOutput("A breakpoint failed whilst executing hit condition");
                args.setLine(breakpoint.getLine());
                args.setSource(breakpoint.getSource());
                return LuaValue.ZERO;
            });
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
        String output = owner.executor.withInterpolation(message, frame, args -> {
            args.setOutput("A breakpoint failed whilst executing log interpolations");
            args.setLine(breakpoint.getLine());
            args.setSource(breakpoint.getSource());
            return LuaValue.NIL;
        });
        outputEventArguments.setOutput(output);
        outputEventArguments.setLine(breakpoint.getLine());
        outputEventArguments.setSource(breakpoint.getSource());
        owner.client.output(outputEventArguments);
        FiguraExtras.sendBrandedMessage("Breakpoint", output);
        return false;
    }
}
