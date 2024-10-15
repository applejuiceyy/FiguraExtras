package com.github.applejuiceyy.figuraextras.util;

import com.github.applejuiceyy.figuraextras.mixin.figura.printer.FiguraLuaPrinterAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Tuple;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.figuramc.figura.lua.LuaTypeManager;
import org.joml.Matrix4f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Util {
    public static void setupTransforms(Window window) {
        Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float) ((double) window.getGuiScaledWidth()), (float) ((double) window.getGuiScaledHeight()), 0.0F, 1000.0F, 21000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.setIdentity();
        poseStack.translate(0.0F, 0.0F, -11000.0F);
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    public static void endTransforms() {
        PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public static <V, T> T with(Consumer<V> setter, Supplier<V> getter, V tempValue, Supplier<T> execution) {
        V original = getter.get();
        setter.accept(tempValue);
        T result = execution.get();
        setter.accept(original);
        return result;
    }

    public static <V> void with(Consumer<V> setter, Supplier<V> getter, V tempValue, Runnable execution) {
        with(setter, getter, tempValue, () -> {
            execution.run();
            return null;
        });
    }

    public static <V, T> T with(SetterGetter<V> setterGetter, V tempValue, Supplier<T> execution) {
        V original = setterGetter.get();
        setterGetter.set(tempValue);
        T result = execution.get();
        setterGetter.set(original);
        return result;
    }

    public static <V> void with(SetterGetter<V> setterGetter, V tempValue, Runnable execution) {
        with(setterGetter, tempValue, () -> {
            execution.run();
            return null;
        });
    }

    public static <T> CompletableFuture<T> fail(ResponseErrorCode code, String message) {
        return CompletableFuture.failedFuture(new ResponseErrorException(new ResponseError(code, message, null)));
    }

    public static Component appendReference(LuaTypeManager typeManager, LuaValue value, Component suffix, boolean quoteStrings) {
        MutableComponent component = FiguraLuaPrinterAccessor.invokeGetPrintText(typeManager, value, false, quoteStrings);
        if (component == null) {
            return suffix;
        }
        return component.append(suffix);
    }

    public static <T> void subscribeIfNeeded(Observers.Observer<T> observer, Event<Runnable>.Source source, Runnable toRun) {
        observer.shouldListen().subscribe(() -> source.subscribe(toRun));
        observer.shouldStopListen().subscribe(() -> source.unsubscribe(toRun));
    }

    public static void pipeObservation(Observers.Observer<?> in, Observers.Observer<?> out) {
        Observers.ConditionalObservation<?> o = out.conditionalObservation(ignored -> {
        });
        in.shouldListen().subscribe(o::start);
        in.shouldStopListen().subscribe(o::stop);
    }

    public static Thread thread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }

    public static Thread after(Runnable runnable, long millis) {
        return thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                return;
            }
            runnable.run();
        });
    }

    public static SafeCloseable maybeTry(Supplier<SafeCloseable> in, boolean yes) {
        if (yes) {
            return in.get();
        }
        return () -> {
        };
    }

    public static Iterable<Tuple<LuaValue, LuaValue>> iterateLua(LuaValue value) {
        return iterateLua(value, LuaValue::next);
    }

    public static Iterable<Tuple<LuaValue, LuaValue>> iterateLua(LuaValue value, BiFunction<LuaValue, LuaValue, Varargs> pair) {
        return () -> new Iterator<>() {
            LuaValue k = LuaValue.NIL;
            Varargs currentResult = null;

            @Override
            public boolean hasNext() {
                return !fetch().arg1().isnil();
            }

            @Override
            public Tuple<LuaValue, LuaValue> next() {
                Varargs n = fetch();
                LuaValue key = n.arg1();
                if (key.isnil())
                    throw new NoSuchElementException();
                currentResult = null;
                k = key;
                LuaValue v = n.arg(2);
                return new Tuple<>(key, v);
            }

            private Varargs fetch() {
                if (currentResult == null) {
                    currentResult = pair.apply(value, k);
                }
                return currentResult;
            }
        };
    }

    static public void closeMultiple(AutoCloseable... closers) {
        int fails = 0;
        RuntimeException exception = null;
        Exception last = null;
        for (AutoCloseable closer : closers) {
            try {
                closer.close();
            } catch (Exception e) {
                if (fails == 0) {
                    exception = new RuntimeException(e);
                } else {
                    if (fails == 1) {
                        exception = new RuntimeException("Failed to close multiple things");
                    }
                    exception.addSuppressed(last);
                }
                last = e;
                fails++;
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    public static byte[] hashBytes(byte[] bytes) {
        MessageDigest instance;
        try {
            instance = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return instance.digest(bytes);
    }

    public interface SetterGetter<T> {
        static <V> SetterGetter<V> value(V initial) {
            return new SetterGetter<>() {
                V v = initial;

                @Override
                public void set(V value) {
                    v = value;
                }

                @Override
                public V get() {
                    return v;
                }
            };
        }

        void set(T value);

        T get();
    }
}
