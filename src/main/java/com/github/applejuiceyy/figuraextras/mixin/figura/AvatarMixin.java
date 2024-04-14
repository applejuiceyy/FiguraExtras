package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.SoundBufferAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Registration;
import com.github.applejuiceyy.figuraextras.tech.trees.dummy.DummyExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.lua.LuaClosureExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.lua.LuaTableExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.lua.LuaValueInterpreter;
import com.github.applejuiceyy.figuraextras.tech.trees.lua.UserdataExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.modelpart.ModelPartExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.modelpart.ModelPartInterpreter;
import com.github.applejuiceyy.figuraextras.tech.trees.modelpart.ModelPartRenderTaskExpander;
import com.github.applejuiceyy.figuraextras.tech.trees.modelpart.RenderTaskInterpreter;
import com.github.applejuiceyy.figuraextras.tech.trees.objects.ObjectScraperExpander;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.util.TriConsumer;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.model.rendering.AvatarRenderer;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

@Mixin(value = Avatar.class, remap = false)
public class AvatarMixin implements AvatarAccess {
    @Shadow
    public FiguraLuaRuntime luaRuntime;
    @Shadow
    public AvatarRenderer renderer;

    @SuppressWarnings("unchecked")
    @Unique
    Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> chatRedirector = new Event<>((subs, firing) -> (a, b) -> {
        firing.startFiring();
        boolean b1 = subs.stream().allMatch(p -> p.test(a, b));
        firing.finishFiring();
        return b1;
    });
    @Unique
    Event<TriConsumer<CompletableFuture<HttpResponse<InputStream>>, HttpRequest, CompletableFuture<String>>> networkEvent = Event.triConsumer();
    @Unique
    Event<Runnable> objectRootUpdater = Event.runnable();
    @Unique
    Event<Runnable> modelRootUpdater = Event.runnable();
    @Unique
    Observers.WritableObserver<Optional<LuaValue>> objectRootObserver = Observers.of(Optional.empty());
    @Unique
    Observers.WritableObserver<Optional<DummyExpander.Dummy>> modelRootObserver = Observers.of(Optional.empty());
    @Unique
    Expander<LuaValue> objectRoot = new Expander<>(
            objectRootObserver,
            Registration.from(registration -> {
                Avatar avatar = (Avatar) (Object) this;
                registration.addExpander(new LuaClosureExpander());
                registration.addExpander(new LuaTableExpander(avatar));
                registration.addExpander(new UserdataExpander());
                registration.addExpander(new ObjectScraperExpander(avatar));
                registration.addInterpreter(new ModelPartInterpreter());
                registration.addInterpreter(new LuaValueInterpreter(avatar));
            }),
            objectRootUpdater.getSource());

    @Unique
    Expander<DummyExpander.Dummy> modelRoot = new Expander<>(
            modelRootObserver,
            Registration.from(registration -> {
                Avatar avatar = (Avatar) (Object) this;
                registration.addExpander(new LuaClosureExpander());
                registration.addExpander(new LuaTableExpander(avatar));
                registration.addExpander(new UserdataExpander());
                // registration.addExpander(new ObjectScraperExpander(avatar));
                registration.addExpander(new ModelPartExpander());
                registration.addExpander(new ModelPartRenderTaskExpander());
                registration.addExpander(new DummyExpander());
                registration.addInterpreter(new ModelPartInterpreter());
                registration.addInterpreter(new RenderTaskInterpreter());
                registration.addInterpreter(new LuaValueInterpreter(avatar));
            }),
            modelRootUpdater.getSource());
    @Unique
    boolean cleaned = false;

    @Inject(method = "clean", at = @At("HEAD"))
    void b(CallbackInfo ci) {
        cleaned = true;
    }

    @Inject(method = "run", at = @At("RETURN"))
    void c(Object toRun, Avatar.Instructions limit, Object[] args, CallbackInfoReturnable<Varargs> cir) {
        objectRootObserver.set(Optional.ofNullable(
                luaRuntime == null ? null : ((LuaRuntimeAccessor) luaRuntime).getUserGlobals())
        );
        modelRootObserver.set(Optional.ofNullable(
                renderer == null ? null : new DummyExpander.Dummy(renderer.root)
        ));
        objectRootUpdater.getSink().run();
        modelRootUpdater.getSink().run();
    }

    @Inject(method = "loadSound", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    void e(String name, byte[] data, CallbackInfo ci, ByteArrayInputStream inputStream, OggAudioStream oggAudioStream, SoundBuffer sound) {
        ((SoundBufferAccess) sound).figuraExtrass$keepBuffer();
    }


    @Override
    public boolean figuraExtrass$isCleaned() {
        return cleaned;
    }

    @Override
    public Expander<LuaValue> figuraExtrass$getObjectViewTree() {
        return objectRoot;
    }

    @Override
    public Expander<DummyExpander.Dummy> figuraExtrass$getModelViewTree() {
        return modelRoot;
    }

    @Override
    public Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> figuraExtrass$getChatRedirect() {
        return chatRedirector;
    }

    @Override
    public Event<TriConsumer<CompletableFuture<HttpResponse<InputStream>>, HttpRequest, CompletableFuture<String>>> figuraExtrass$getNetworkLogger() {
        return networkEvent;
    }
}
