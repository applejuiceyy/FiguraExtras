package com.github.applejuiceyy.figuraextras.tech.captures;

import com.github.applejuiceyy.figuraextras.ducks.statics.LuaDuck;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.lang.reflect.Method;

public class SingularCapture implements Hook {

    private final Hook in;
    private final Runnable destroy;

    public SingularCapture(Hook in, Runnable destroy) {
        this.in = in;
        this.destroy = destroy;
    }

    @Override
    public void intoFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, LuaDuck.CallType type, String possibleName) {
        in.intoFunction(luaClosure, varargs, stack, type, possibleName);
    }

    @Override
    public void outOfFunction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, Object returns, LuaDuck.ReturnType type) {
        in.outOfFunction(luaClosure, varargs, stack, returns, type);
    }

    @Override
    public void instruction(LuaClosure luaClosure, Varargs varargs, LuaValue[] stack, int instruction, int pc) {
        in.instruction(luaClosure, varargs, stack, instruction, pc);
    }

    @Override
    public void end() {
        in.end();
        destroy.run();
    }

    @Override
    public void endError(Object err) {
        in.endError(err);
        destroy.run();
    }

    @Override
    public void startEvent(String runReason, Object toRun, Varargs val) {
        in.startEvent(runReason, toRun, val);
    }

    @Override
    public void startInit(String name) {
        in.startInit(name);
    }

    @Override
    public void marker(String name) {
        in.marker(name);
    }

    @Override
    public void region(String regionName) {
        in.region(regionName);
    }

    @Override
    public void intoJavaFunction(Varargs args, Method val$method, LuaDuck.CallType type) {
        in.intoJavaFunction(args, val$method, type);
    }

    @Override
    public void outOfJavaFunction(Varargs args, Method val$method, Object result, LuaDuck.ReturnType type) {
        in.outOfJavaFunction(args, val$method, result, type);
    }

    @Override
    public void intoPCall() {
        in.intoPCall();
    }

    @Override
    public void outOfPCall() {
        in.outOfPCall();
    }
}
