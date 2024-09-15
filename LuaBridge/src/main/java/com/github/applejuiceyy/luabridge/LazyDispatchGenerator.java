package com.github.applejuiceyy.luabridge;

import com.google.common.collect.ImmutableList;
import org.luaj.vm2.Varargs;

public class LazyDispatchGenerator implements DispatchGenerator {

    private final DispatchGenerator up;

    LazyDispatchGenerator(DispatchGenerator up) {
        this.up = up;
    }

    @Override
    public Dispatch generateDispatch(OverloadedMethod method, ImmutableList<OverloadedMethod.Overload> node) {
        return new Dispatch() {
            Dispatch upstream = null;

            @Override
            public Varargs dispatch(LuaRuntime<?> runtime, Bridge bridge, Varargs varargs) {
                if (upstream == null) {
                    upstream = up.generateDispatch(method, node);
                }
                return upstream.dispatch(runtime, bridge, varargs);
            }
        };
    }
}
