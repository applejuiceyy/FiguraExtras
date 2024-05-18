package com.github.applejuiceyy.figuraextras.mixin.figura.lua;

import com.github.applejuiceyy.figuraextras.ducks.GlobalsAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaRuntimeAccess;
import com.github.applejuiceyy.figuraextras.ducks.LuaTypeManagerAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.LuaRuntimeDuck;
import com.github.applejuiceyy.figuraextras.ipc.dsp.SourceListener;
import com.github.applejuiceyy.figuraextras.lua.DebuggerAPI;
import com.github.applejuiceyy.figuraextras.tech.captures.Hook;
import com.github.applejuiceyy.figuraextras.util.Event;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Tuple;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.lua.api.event.EventsAPI;
import org.figuramc.figura.lua.api.event.LuaEvent;
import org.luaj.vm2.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = FiguraLuaRuntime.class, remap = false)
public abstract class LuaRuntimeMixin implements LuaRuntimeAccess {
    @Unique
    int initCount = -1;
    @Unique
    Event<SourceListener> dynamicLoadEvent = Event.interfacing(SourceListener.class);

    @Shadow
    public abstract void setGlobal(String name, Object obj);

    @Shadow
    @Final
    public LuaTypeManager typeManager;
    @Shadow
    @Final
    private Globals userGlobals;
    @Shadow
    public EventsAPI events;
    @Unique
    private WeakHashMap<Prototype, Integer> prototypesMarkedAsLoadStringed;
    @Unique
    private HashMap<Integer, Tuple<String, String>> sourceName;
    @Unique
    private int nextSource = 1;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Map;putAll(Ljava/util/Map;)V"))
    void init(Avatar avatar, Map<String, String> scripts, CallbackInfo ci) {
        ((LuaTypeManagerAccess) typeManager).figuraExtrass$setAvatar(avatar);
        prototypesMarkedAsLoadStringed = new WeakHashMap<>();
        sourceName = new HashMap<>();
        nextSource = 1;
    }

    @Inject(method = "loadExtraLibraries", at = @At("TAIL"))
    void e(CallbackInfo ci) {
        setGlobal("debugger", typeManager.javaToLua(new DebuggerAPI((FiguraLuaRuntime) (Object) this)));
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/lua/FiguraLuaRuntime;initializeScript(Ljava/lang/String;)Lorg/luaj/vm2/Varargs;"))
    void initStart(ListTag autoScripts, CallbackInfoReturnable<Boolean> cir) {
        initCount = 0;
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/lua/FiguraLuaRuntime;error(Ljava/lang/Throwable;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    void initError(ListTag autoScripts, CallbackInfoReturnable<Boolean> cir, Throwable e) {
        Hook hook = ((GlobalsAccess) userGlobals).figuraExtrass$getCaptureState().getSink();
        if (hook != null) {
            hook.endError(e);
        }
    }

    @Inject(method = "initializeScript", at = @At(value = "INVOKE", target = "Lorg/luaj/vm2/LuaValue;invoke(Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;"))
    void initScript(String str, CallbackInfoReturnable<Varargs> cir) {
        if (initCount != -1) {
            if (initCount == 0) {
                Hook hook = ((GlobalsAccess) userGlobals).figuraExtrass$getCaptureState().getSink();
                if (hook != null) {
                    hook.startInit(str);
                }
            }
            initCount++;
        }
    }

    @Inject(method = "initializeScript", at = @At(value = "INVOKE", target = "Lorg/luaj/vm2/LuaValue;invoke(Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", shift = At.Shift.AFTER))
    void initScriptEnd(String str, CallbackInfoReturnable<Varargs> cir) {
        if (initCount == -1) return;
        initCount--;
        if (initCount == 0) {
            initCount = -1;
            Hook hook = ((GlobalsAccess) userGlobals).figuraExtrass$getCaptureState().getSink();
            if (hook != null) {
                hook.end();
            }
        }
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/lua/FiguraLuaRuntime;setInstructionLimit(I)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    void e(Object toRun, Avatar.Instructions limit, Object[] args, CallbackInfoReturnable<Varargs> cir, LuaValue[] values, Varargs val) {

        ((GlobalsAccess) userGlobals)
                .figuraExtrass$getCaptureState()
                .startEvent(toRun);

        Hook hook = ((GlobalsAccess) userGlobals).figuraExtrass$getCaptureState().getSink();
        if (hook != null) {
            String reason = "Unknown";
            if (LuaRuntimeDuck.runReason != null) {
                reason = LuaRuntimeDuck.runReason;
                LuaRuntimeDuck.runReason = null;
            } else if (toRun instanceof String str) {
                reason = str.toLowerCase().replace('_', ' ');
            } else if (toRun instanceof LuaEvent ev) {
                reason = events.getEvents()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() == ev)
                        .findFirst()
                        .map(e -> "Event " + e.getKey())
                        .orElse("Event");
            } else if (toRun instanceof LuaValue ev) {
                reason = "Execution of " + ev;
            }
            hook.startEvent(reason, toRun, val);
        }
    }

    @Inject(method = "run", at = @At(value = "RETURN", ordinal = 0))
    void runNormal(Object toRun, Avatar.Instructions limit, Object[] args, CallbackInfoReturnable<Varargs> cir) {
        Hook capture = ((GlobalsAccess) userGlobals).figuraExtrass$getCaptureState().getSink();
        if (capture != null) {
            capture.end();
        }
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lorg/figuramc/figura/lua/FiguraLuaRuntime;error(Ljava/lang/Throwable;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    void runFail(Object toRun, Avatar.Instructions limit, Object[] args, CallbackInfoReturnable<Varargs> cir, LuaValue[] values, Varargs val, Throwable e) {
        Hook capture = ((GlobalsAccess) userGlobals).figuraExtrass$getCaptureState().getSink();
        if (capture != null) {
            capture.endError(e);
        }
    }

    @Redirect(method = "loadExtraLibraries", at = @At(value = "INVOKE", target = "Lorg/luaj/vm2/Globals;load(Ljava/lang/String;Ljava/lang/String;)Lorg/luaj/vm2/LuaValue;"))
    LuaValue l(Globals instance, String script, String chunkname) {
        LuaValue loaded = instance.load(script, chunkname);

        if (loaded instanceof LuaClosure closure) {
            this.figuraExtrass$newDynamicLoad(closure.p, script);
        }
        ;
        return loaded;
    }

    @Override
    public WeakHashMap<Prototype, Integer> figuraExtrass$getPrototypesMarkedAsLoadStringed() {
        return prototypesMarkedAsLoadStringed;
    }

    @Override
    public HashMap<Integer, Tuple<String, String>> figuraExtrass$getRegisteredDynamicSources() {
        return sourceName;
    }

    @Override
    public int figuraExtrass$newDynamicLoad(Prototype prototype, String source) {
        if (sourceName.size() > 10) {
            IntArraySet seen = new IntArraySet();
            for (Map.Entry<Prototype, Integer> entry : prototypesMarkedAsLoadStringed.entrySet()) {
                seen.add(entry.getValue().intValue());
            }
            for (Iterator<Integer> iterator = sourceName.keySet().iterator(); iterator.hasNext(); ) {
                Integer integer = iterator.next();
                if (!seen.contains(integer.intValue())) {
                    dynamicLoadEvent.getSink().removed(sourceName.get(integer).getB(), sourceName.get(integer).getA(), integer);
                    iterator.remove();
                }
            }
        }
        sourceName.put(nextSource++, new Tuple<>(source, prototype.source.tojstring()));
        propagateMarking(prototype, nextSource - 1);
        dynamicLoadEvent.getSink().added(prototype.source.tojstring(), source, nextSource - 1);
        return nextSource - 1;
    }

    @Unique
    void propagateMarking(Prototype prototype, int source) {
        prototypesMarkedAsLoadStringed.put(prototype, source);
        for (Prototype child : prototype.p) {
            propagateMarking(child, source);
        }
    }

    @Override
    public String figuraExtrass$getSource(int i) {
        return sourceName.get(i).getA();
    }

    @Override
    public Event<SourceListener>.Source figuraExtrass$dynamicLoadsEvent() {
        return dynamicLoadEvent.getSource();
    }
}
