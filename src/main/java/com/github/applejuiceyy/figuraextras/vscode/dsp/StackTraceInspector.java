package com.github.applejuiceyy.figuraextras.vscode.dsp;

import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.util.Tuple;
import org.eclipse.lsp4j.debug.*;
import org.luaj.vm2.LocVars;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class StackTraceInspector {

    private final StackTraceTracker stackTrace;
    private final ArrayList<StackFrame> frames;
    private final Int2IntOpenHashMap idMapping = new Int2IntOpenHashMap();

    private final ArrayList<Supplier<Variable[]>> generatedVariablesReference = new ArrayList<>();
    private final DebugProtocolServer owner;

    public StackTraceInspector(DebugProtocolServer owner, StackTraceTracker stackTrace) {
        this.stackTrace = stackTrace;
        this.owner = owner;
        generatedVariablesReference.add(null);

        frames = new ArrayList<>();
        IdGenerator frameIdGenerator = new IdGenerator();

        List<Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame>> frameList = stackTrace.frameList;
        for (int i = 0; i < frameList.size(); i++) {
            Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame> frame = frameList.get(i);
            int id = frameIdGenerator.getAsInt();

            StackFrame stackFrame = new StackFrame();
            stackFrame.setId(id);
            idMapping.put(id, i);

            if (frame.right().isPresent()) {
                StackTraceTracker.LuaFrame luaFrame = frame.right().get();

                stackFrame.setName(luaFrame.possibleName == null ? luaFrame.closure.name() : luaFrame.possibleName);
                stackFrame.setLine(luaFrame.line);
                stackFrame.setSource(owner.sourcer.toSource(luaFrame.closure.p));

                if (luaFrame.callType == LuaDuck.CallType.TAIL) {
                    StackFrame virtualFrame = new StackFrame();
                    virtualFrame.setName("Tail call");
                    virtualFrame.setPresentationHint(StackFramePresentationHint.LABEL);
                    frames.add(0, virtualFrame);
                }
            } else if (frame.left().isPresent()) {
                StackTraceTracker.JavaFrame javaFrame = frame.left().get();

                stackFrame.setName(javaFrame.javaFrame);
                stackFrame.setPresentationHint(StackFramePresentationHint.SUBTLE);
            }

            frames.add(0, stackFrame);
        }


        StackFrame stackFrame = new StackFrame();
        stackFrame.setName(stackTrace.kickstarter);
        stackFrame.setPresentationHint(StackFramePresentationHint.LABEL);
        stackFrame.setId(frameIdGenerator.getAsInt());

        frames.add(stackFrame);
    }

    private <T> T handle(int frameId, Function<StackTraceTracker.LuaFrame, T> ifLua, Function<StackTraceTracker.JavaFrame, T> ifJava) {
        int i = idMapping.get(frameId);
        Either<StackTraceTracker.JavaFrame, StackTraceTracker.LuaFrame> frame = stackTrace.frameList.get(i);
        if (frame.right().isPresent()) {
            return ifLua.apply(frame.right().get());
        }
        if (frame.left().isPresent()) {
            return ifJava.apply(frame.left().get());
        }
        throw new RuntimeException();  // unreachable
    }

    private String reasonableTypeName(LuaValue value) {
        if (value.isstring() && !(value instanceof LuaNumber)) {
            return "\"" + value + "\"";
        } else {
            return value.toString();
        }
    }

    private Variable processValue(LuaValue value, String varName) {
        Variable variable = new Variable();
        variable.setName(varName);
        return processValue(value, variable);
    }

    private Variable processValue(LuaValue value, Variable variable) {
        String interpreted = value.toString();
        try {
            interpreted = VariableRenderer.interpret(value);
        } catch (LuaError err) {
            OutputEventArguments outputEventArguments = new OutputEventArguments();
            outputEventArguments.setCategory(OutputEventArgumentsCategory.IMPORTANT);
            outputEventArguments.setOutput("Error while evaluating: " + err.getMessage());
            owner.client.output(outputEventArguments);
        }

        variable.setValue(interpreted);
        VariablePresentationHint variablePresentationHint = new VariablePresentationHint();
        variable.setPresentationHint(variablePresentationHint);

        if (owner.clientCapabilities.getSupportsVariableType()) {
            variable.setType(VariableRenderer.getType(owner.getCurrentAvatar(), value));
        }
        Supplier<List<Tuple<Variable, LuaValue>>> expander = null;
        try {
            expander = VariableRenderer.expand(owner.getCurrentAvatar(), value);
        } catch (LuaError err) {
            OutputEventArguments outputEventArguments = new OutputEventArguments();
            outputEventArguments.setCategory(OutputEventArgumentsCategory.IMPORTANT);
            outputEventArguments.setOutput("Error while evaluating: " + err.getMessage());
            owner.client.output(outputEventArguments);
        }

        if (expander != null) {
            variable.setVariablesReference(generatedVariablesReference.size());
            Supplier<List<Tuple<Variable, LuaValue>>> finalExpander = expander;
            generatedVariablesReference.add(() -> {
                List<Tuple<Variable, LuaValue>> list = finalExpander.get();
                List<Variable> variables = new ArrayList<>();
                list.forEach(tuple -> {
                    variables.add(tuple.getA());
                    processValue(tuple.getB(), tuple.getA());
                });
                return variables.toArray(new Variable[0]);
            });
        }

        return variable;
    }

    public StackFrame[] getStackTrace(int startRange, int endRange) {
        endRange = Math.min(frames.size(), endRange);
        startRange = Math.max(0, startRange);  // realistically this is only possible if startRange is negative
        return frames.subList(startRange, endRange).toArray(new StackFrame[0]);
    }

    public Scope[] getScopes(int frameId) {
        return handle(frameId, this::getLuaScopes, this::getJavaScopes);
    }

    private Scope[] getJavaScopes(StackTraceTracker.JavaFrame o) {
        Scope scope = new Scope();
        scope.setName("Arguments");
        scope.setPresentationHint(ScopePresentationHint.ARGUMENTS);
        scope.setVariablesReference(generatedVariablesReference.size());
        generatedVariablesReference.add(() -> {
            List<Variable> variables = new ArrayList<>();
            for (int i = 1; i <= o.arguments.narg(); i++) {
                LuaValue value = o.arguments.arg(i);
                variables.add(processValue(value, "argument" + i));
            }
            return variables.toArray(new Variable[0]);
        });
        return new Scope[]{scope};
    }

    private Scope[] getLuaScopes(StackTraceTracker.LuaFrame o) {
        Scope globalsScope = new Scope();
        globalsScope.setName("Globals");
        globalsScope.setExpensive(true);
        globalsScope.setVariablesReference(generatedVariablesReference.size());
        generatedVariablesReference.add(() -> {
            if (o.closure.upValues == null || o.closure.upValues.length == 0) {
                return new Variable[0];
            }
            LuaValue globals = o.closure.upValues[0].getValue();
            Supplier<List<Tuple<Variable, LuaValue>>> expander = null;
            try {
                expander = VariableRenderer.expand(owner.getCurrentAvatar(), globals);
            } catch (LuaError err) {
                OutputEventArguments outputEventArguments = new OutputEventArguments();
                outputEventArguments.setCategory(OutputEventArgumentsCategory.IMPORTANT);
                outputEventArguments.setOutput("Error while evaluating: " + err.getMessage());
                owner.client.output(outputEventArguments);
            }
            if (expander != null) {
                List<Tuple<Variable, LuaValue>> list = expander.get();
                List<Variable> vars = new ArrayList<>();
                list.forEach(tuple -> {
                    vars.add(tuple.getA());
                    processValue(tuple.getB(), tuple.getA());
                });
                return vars.toArray(new Variable[0]);
            } else {
                return new Variable[0];
            }
        });

        Scope argumentScope = new Scope();
        argumentScope.setName("Arguments");
        argumentScope.setPresentationHint(ScopePresentationHint.ARGUMENTS);
        argumentScope.setVariablesReference(generatedVariablesReference.size());
        generatedVariablesReference.add(() -> {
            List<Variable> variables = new ArrayList<>();
            for (int i = 0; i < o.closure.p.numparams; i++) {
                LuaValue value = o.stack[i];
                variables.add(processValue(value, o.closure.p.locvars[i].varname.tojstring()));
            }
            return variables.toArray(new Variable[0]);
        });

        Scope upvalueScope = new Scope();
        upvalueScope.setName("Upvalues");
        upvalueScope.setVariablesReference(generatedVariablesReference.size());
        generatedVariablesReference.add(() -> {
            List<Variable> variables = new ArrayList<>();
            for (int i = 1; i < o.closure.p.upvalues.length; i++) {
                LuaValue value = o.closure.upValues[i].getValue();
                variables.add(processValue(value, o.closure.p.upvalues[i].name.tojstring()));
            }
            return variables.toArray(new Variable[0]);
        });

        Scope localScope = new Scope();
        localScope.setName("Locals");
        localScope.setPresentationHint(ScopePresentationHint.LOCALS);
        localScope.setVariablesReference(generatedVariablesReference.size());
        generatedVariablesReference.add(() -> {
            List<Variable> variables = new ArrayList<>();
            List<String> activeVariables = new ArrayList<>();

            for (LocVars var : o.closure.p.locvars) {
                if (var.startpc <= o.pc && var.endpc >= o.pc) {
                    activeVariables.add(var.varname.tojstring());
                }
            }

            if (o.closure.p.numparams > 0) {
                activeVariables.subList(0, o.closure.p.numparams).clear();
            }

            for (int i = 0; i < activeVariables.size(); i++) {
                LuaValue value = o.stack[i + o.closure.p.numparams];
                String name = activeVariables.get(i);
                Variable e = processValue(value, name);
                // synthetic variable introduced by for loops
                if (name.startsWith("(")) {
                    VariablePresentationHint variablePresentationHint = e.getPresentationHint();
                    variablePresentationHint.setKind(VariablePresentationHintKind.VIRTUAL);
                    variablePresentationHint.setVisibility(VariablePresentationHintVisibility.INTERNAL);
                    e.setPresentationHint(variablePresentationHint);
                }
                variables.add(e);
            }
            return variables.toArray(new Variable[0]);
        });


        Scope stackScope = new Scope();
        stackScope.setName("Stack");
        stackScope.setPresentationHint(ScopePresentationHint.REGISTERS);
        stackScope.setVariablesReference(generatedVariablesReference.size());
        generatedVariablesReference.add(() -> {
            List<Variable> variables = new ArrayList<>();
            List<String> activeVariables = new ArrayList<>();

            for (LocVars var : o.closure.p.locvars) {
                if (var.startpc <= o.pc && var.endpc >= o.pc) {
                    activeVariables.add(var.varname.tojstring());
                }
            }

            for (int i = activeVariables.size(); i < o.stack.length; i++) {
                LuaValue value = o.stack[i];
                variables.add(processValue(value, "Stack Slot " + (i - activeVariables.size() + 1)));
            }
            return variables.toArray(new Variable[0]);
        });

        if (o.closure.p.is_vararg > 0) {
            Scope varargScope = new Scope();
            varargScope.setName("Argument Varargs");
            varargScope.setPresentationHint(ScopePresentationHint.ARGUMENTS);
            varargScope.setVariablesReference(generatedVariablesReference.size());
            generatedVariablesReference.add(() -> {
                List<Variable> variables = new ArrayList<>();
                for (int i = 1; i <= o.varargs.narg(); i++) {
                    LuaValue value = o.varargs.arg(i);
                    variables.add(processValue(value, "Position " + i));
                }
                return variables.toArray(new Variable[0]);
            });
            return new Scope[]{globalsScope, argumentScope, varargScope, upvalueScope, localScope, stackScope};
        }

        return new Scope[]{globalsScope, argumentScope, upvalueScope, localScope, stackScope};
    }

    public Variable[] getVariables(int variablesReference, int startRange, int endRange) {
        Variable[] variables = generatedVariablesReference.get(variablesReference).get();
        if (variables.length == 0) {
            return variables;
        }
        endRange = Math.min(variables.length, endRange);
        startRange = Math.max(0, startRange);  // realistically this is only possible if startRange is negative
        if (endRange == variables.length && startRange == 0) {
            return variables;
        }
        Variable[] out = new Variable[endRange - startRange];
        System.arraycopy(variables, startRange, out, 0, endRange - startRange);
        return out;
    }

    public EvaluateResponse evaluateExpression(EvaluateArguments args) {
        LuaValue value = owner.executor.composeAndCall(args.getExpression(), stackTrace.frameList.get(stackTrace.frameList.size() - 1), a -> {
            a.setOutput("Error while evaluating");
            return LuaValue.NIL;
        });
        Variable var = processValue(value, "");
        EvaluateResponse evaluateResponse = new EvaluateResponse();
        evaluateResponse.setResult(var.getValue());
        evaluateResponse.setType(var.getType());
        evaluateResponse.setVariablesReference(var.getVariablesReference());
        return evaluateResponse;
    }

    public int getStackTraceSize() {
        return frames.size();
    }
}
