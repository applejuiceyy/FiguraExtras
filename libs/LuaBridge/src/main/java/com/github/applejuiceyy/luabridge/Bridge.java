package com.github.applejuiceyy.luabridge;

import com.github.applejuiceyy.luabridge.annotation.LuaClass;
import com.github.applejuiceyy.luabridge.annotation.LuaMetatable;
import com.github.applejuiceyy.luabridge.annotation.LuaMethod;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Bridge implements OverloadedMethodHook {

    private final Set<Class<?>> types;
    private final ClassMap<Class<?>> wrapping;
    private final DispatchGenerator generator;
    private final ClassMap<LuaValue> processedTypes = new ClassMap<>();
    private LuaRuntime<?> luaRuntime;

    public static BridgeBuilder<Bridge> create() {
        return new BridgeBuilder<>(Bridge::new);
    }

    protected Bridge(Set<Class<?>> types, ClassMap<Class<?>> wrapping, DispatchGenerator generator) {
        this.types = types;
        this.wrapping = wrapping;
        this.generator = generator;
    }

    public static <V extends Bridge> BridgeBuilder<V> create(BridgeConstructor<V> builder) {
        return new BridgeBuilder<>(builder);
    }

    void setup() {
        for (Class<?> type : types) {
            Class<?> key = getWrapping(type);
            processClass(type, key == null ? type : key);
        }
    }

    private @Nullable Class<?> getWrapping(Class<?> type) {
        LuaClass annotation = type.getAnnotation(LuaClass.class);
        Class<?> wraps = annotation.wraps();
        return wraps == Object.class ? null : wraps;
    }

    private void processClass(Class<?> cls, Class<?> wrapping) {
        ArrayList<Collector<Method>> list = new ArrayList<>();
        attachCollectors(cls, wrapping, list::add);
        this.wrapping.findMapped(wrapping, c -> {
            collect(c, method -> list.forEach(p -> p.collect(method)));
        });
        list.forEach(Collector::end);
    }

    protected void attachCollectors(Class<?> cls, Class<?> wrapping, Consumer<Collector<Method>> collectorConsumer) {
        collectorConsumer.accept(new Collector<>() {
            final Map<String, OverloadedMethod> methods = new HashMap<>();
            final Map<String, OverloadedMethod> metaMethods = new HashMap<>();

            @Override
            public void collect(Method method) {
                String name = method.getName();
                if (method.getAnnotation(LuaMethod.class) != null) {
                    OverloadedMethod overloadedMethod = methods.computeIfAbsent(
                            name,
                            (k) -> new OverloadedMethod(name, Bridge.this)
                    );
                    try {
                        overloadedMethod.addOverload(method);
                    } catch (IllegalAccessException ignored) {
                    }
                }
                if (method.getAnnotation(LuaMetatable.class) != null) {
                    OverloadedMethod overloadedMethod = metaMethods.computeIfAbsent(
                            name,
                            (k) -> new OverloadedMethod(name, Bridge.this)
                    );
                    try {
                        overloadedMethod.addOverload(method);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }

            @Override
            public void end() {
                Map<String, Function<Varargs, Varargs>> compiledMethods = methods
                        .entrySet()
                        .stream()
                        .map(e -> Map.entry(e.getKey(), e.getValue().generateDispatch()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Map<String, Function<Varargs, Varargs>> compiledMetaMethods = metaMethods
                        .entrySet()
                        .stream()
                        .map(e -> Map.entry(e.getKey(), e.getValue().generateDispatch()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                LuaTable indexTable = new LuaTable();
                LuaValue indexFunction;
                for (Map.Entry<String, Function<Varargs, Varargs>> entry : compiledMethods.entrySet()) {
                    Function<Varargs, Varargs> compiled = entry.getValue();
                    indexTable.set(entry.getKey(), new VarArgFunction() {
                        {
                            name = entry.getKey();
                        }

                        @Override
                        public Varargs invoke(Varargs args) {
                            return compiled.apply(args);
                        }
                    });
                }

                if (compiledMetaMethods.containsKey("__index")) {
                    Function<Varargs, Varargs> meta = compiledMetaMethods.get("__index");

                    indexFunction = new VarArgFunction() {
                        {
                            name = "envelopingIndex";
                        }

                        @Override
                        public Varargs invoke(Varargs args) {
                            Varargs apply = indexTable.get(args.arg(2));
                            if (apply.isnil(1)) {
                                return meta.apply(args);
                            }
                            return apply;
                        }
                    };
                } else {
                    indexFunction = indexTable;
                }

                LuaTable ret = new LuaTable();
                ret.set("__index", indexFunction);

                for (Map.Entry<String, Function<Varargs, Varargs>> entry : compiledMetaMethods.entrySet()) {
                    if (entry.getKey().equals("__index")) continue;

                    Function<Varargs, Varargs> compile = entry.getValue();
                    ret.set(entry.getKey(), new VarArgFunction() {
                        {
                            name = entry.getKey();
                        }

                        @Override
                        public Varargs invoke(Varargs args) {
                            return compile.apply(args);
                        }
                    });
                }

                processedTypes.add(wrapping, ret);
            }
        });
    }

    private void collect(Class<?> cls, Consumer<Method> consumer) {
        for (Method method : cls.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                consumer.accept(method);
            }
        }
    }

    public Object toJava(LuaValue val, boolean isIndex) {
        if (val.istable())
            return val.checktable();
        else if (val.isinttype())
            return isIndex ? val.checkint() - 1 : val.checkdouble();
        else if (val.isstring())
            return val.checkjstring();
        else if (val.isboolean())
            return val.checkboolean();
        else if (val.isfunction())
            return val.checkfunction();
        else if (val.isuserdata())
            return val.checkuserdata(Object.class);
        else
            return null;
    }

    public LuaValue toLua(Object val, boolean isIndex) {
        if (val instanceof LuaValue l)
            return l;
        else if (val instanceof Varargs v)
            return v.arg1();
        else if (val instanceof Double d)
            return LuaValue.valueOf(d);
        else if (val instanceof String s)
            return LuaValue.valueOf(s);
        else if (val instanceof Boolean b)
            return LuaValue.valueOf(b);
        else if (val instanceof Integer i)
            return LuaValue.valueOf(isIndex ? i + 1 : i);
        else if (val instanceof Float f)
            return LuaValue.valueOf(f);
        else if (val instanceof Byte b)
            return LuaValue.valueOf(b);
        else if (val instanceof Long l)
            return LuaValue.valueOf(isIndex ? l + 1 : l);
        else if (val instanceof Character c)
            return LuaValue.valueOf(c);
        else if (val instanceof Short s)
            return LuaValue.valueOf(s);

        return wrap(val);
    }

    @Override
    public Varargs toLuaVarargs(boolean isIndex, Object... object) {
        if (object.length == 1) {
            Object o = object[0];
            if (o instanceof Varargs v) {
                return v;
            }
            return toLua(o, isIndex);
        }
        return LuaValue.varargsOf(Arrays.stream(object).map(l -> toLua(l, isIndex)).toArray(LuaValue[]::new));
    }

    public OverloadedMethod createOverload(String name) {
        return new OverloadedMethod(name, this);
    }

    @Override
    public void enter(Varargs args) {

    }

    @Override
    public void enterResolved(String name) {

    }

    @Override
    public void exit(Varargs ret) {

    }

    @Override
    public void error(Throwable th) {

    }

    public LuaValue wrap(Object val) {
        LuaValue first = processedTypes.findFirst(val.getClass());
        if (first == null) {
            throw new LuaError("Class " + val.getClass().getName() + " is not registered");
        }
        return LuaValue.userdataOf(val).setmetatable(first);
    }

    public DispatchGenerator getDispatchGenerator() {
        return generator;
    }

    public void setOwner(LuaRuntime<?> luaRuntime) {
        if (this.luaRuntime != null) {
            throw new IllegalStateException("Bridge is already owned");
        }
        this.luaRuntime = luaRuntime;
    }

    public LuaRuntime<?> getLuaRuntime() {
        return luaRuntime;
    }

    public interface BridgeConstructor<V> {
        V build(Set<Class<?>> types, ClassMap<Class<?>> wrapping, DispatchGenerator generator);
    }

    static public class BridgeBuilder<V extends Bridge> {
        private final Set<Class<?>> types = new HashSet<>();
        private final ClassMap<Class<?>> wrapping = new ClassMap<>();
        private DispatchGenerator generator = null;
        private BridgeConstructor<V> constructor;

        public BridgeBuilder() {
        }

        public BridgeBuilder(BridgeConstructor<V> constructor) {
            this.constructor = constructor;
        }

        public BridgeBuilder<V> generator(DispatchGenerator generator) {
            this.generator = generator;
            return this;
        }


        public BridgeBuilder<V> addClass(Class<?> type) {
            LuaClass annotation = type.getAnnotation(LuaClass.class);
            if (annotation == null) {
                throw new IllegalArgumentException("type " + type.getName() + " is not whitelisted");
            }
            if (annotation.wraps() != Object.class) {
                wrapping.add(annotation.wraps(), type);
            } else {
                wrapping.add(type, type);
            }
            types.add(type);
            return this;
        }


        public V build() {
            Objects.requireNonNull(generator);
            V built = constructor.build(types, wrapping, generator);
            built.setup();
            return built;
        }
    }
}
