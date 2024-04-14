package com.github.applejuiceyy.figuraextras.vscode.dsp;

import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.eclipse.lsp4j.debug.*;
import org.figuramc.figura.math.matrix.FiguraMatrix;
import org.figuramc.figura.math.vector.FiguraVector;
import org.luaj.vm2.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
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
        variable.setValue(reasonableTypeName(value));
        VariablePresentationHint variablePresentationHint = new VariablePresentationHint();
        variable.setPresentationHint(variablePresentationHint);

        if (owner.clientCapabilities.getSupportsVariableType()) {
            String type;
            if (value.isuserdata()) {
                Class<?> cls = value.checkuserdata().getClass();
                if (FiguraMatrix.class.isAssignableFrom(cls) || FiguraVector.class.isAssignableFrom(cls)) {
                    variablePresentationHint.setKind(VariablePresentationHintKind.DATA);
                }
                type = owner.getCurrentAvatar().luaRuntime.typeManager.getTypeName(cls);
            } else {
                type = value.typename();
            }
            variable.setType(type);
        }

        if (value.istable()) {
            variable.setVariablesReference(generatedVariablesReference.size());
            generatedVariablesReference.add(() -> {
                List<Variable> variables = new ArrayList<>();
                LuaValue k = LuaValue.NIL;
                while (true) {
                    Varargs n = value.next(k);
                    if ((k = n.arg1()).isnil())
                        break;
                    LuaValue v = n.arg(2);
                    variables.add(processValue(v, k.toString()));
                }
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
            for (int i = 0; i < o.closure.p.upvalues.length; i++) {
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

        return new Scope[]{argumentScope, upvalueScope, localScope, stackScope};
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
}
