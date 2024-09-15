package com.github.applejuiceyy.luabridge;

import com.google.common.collect.ImmutableList;
import org.luaj.vm2.Varargs;

public interface DispatchGenerator {
    Dispatch generateDispatch(OverloadedMethod method, ImmutableList<OverloadedMethod.Overload> node);

    interface Dispatch {
        Varargs dispatch(LuaRuntime<?> runtime, Bridge bridge, Varargs varargs);
    }
}
