package com.github.applejuiceyy.figuraextras.mixin;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.MinecraftAccess;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.MonitorContentPopOutHost;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements MinecraftAccess {
    @Unique
    WindowContentPopOutHost host;
    @Unique
    long lastFullRender = 0;
    @Unique
    MonitorContentPopOutHost monitorHost;
    @Unique
    Consumer<Screen> screenSetter;
    @Shadow
    @Final
    private VirtualScreen virtualScreen;

    @Shadow
    public abstract Window getWindow();

    @Mutable
    @Shadow
    @Final
    private Window window;

    @Shadow
    public abstract RenderBuffers renderBuffers();

    @Mutable
    @Shadow
    @Final
    private RenderTarget mainRenderTarget;

    @Shadow
    @Final
    public MouseHandler mouseHandler;

    @Shadow
    public abstract float getDeltaFrameTime();

    @Shadow
    private ProfilerFiller profiler;

    @Shadow
    public abstract void destroy();

    @Shadow
    protected abstract ProfilerFiller constructProfiler(boolean active, @Nullable SingleTickProfiler monitor);

    @Override
    public VirtualScreen figuraExtrass$getScreenManager() {
        return virtualScreen;
    }

    @Override
    public <T> T figuraExtrass$withWindow(Window newWindow, RenderTarget target, Supplier<T> runnable) {
        Window actualWindow = window;
        RenderTarget actualTarget = mainRenderTarget;
        window = newWindow;
        mainRenderTarget = target;
        T ret = runnable.get();
        window = actualWindow;
        mainRenderTarget = actualTarget;
        return ret;
    }

    @Override
    public <T> T figuraExtrass$withSetScreen(Consumer<Screen> setScreen, Supplier<T> runnable) {
        Consumer<Screen> original = screenSetter;
        screenSetter = setScreen;
        T ret = runnable.get();
        screenSetter = original;
        return ret;
    }

    @Inject(
            method = "<init>",
            at = @At(value = "TAIL")
    )
    void b(GameConfig args, CallbackInfo ci) {
        host = new WindowContentPopOutHost(window);
        monitorHost = new MonitorContentPopOutHost(window);
    }

    @Inject(
            method = "runTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", ordinal = 6)
    )
    void b(boolean tick, CallbackInfo ci) {
        profiler.push("FiguraExtras Component rendering");
        Util.setupTransforms(window);
        GuiGraphics guiGraphics = new GuiGraphics((Minecraft) (Object) this, this.renderBuffers().bufferSource());
        host.render(guiGraphics, mouseHandler.xpos(), mouseHandler.ypos(), getDeltaFrameTime());
        Util.endTransforms();
        profiler.pop();
    }

    @Inject(
            method = "runTick",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V", shift = At.Shift.AFTER)
    )
    void a(boolean tick, CallbackInfo ci) {
        profiler.popPush("Other windows");
        ArrayList<DetachedWindow> toRemove = new ArrayList<>(0);
        long current = System.currentTimeMillis();
        boolean doRender = false;
        if (lastFullRender < current - 100) {
            lastFullRender = current;
            doRender = true;
        }
        for (DetachedWindow detachedWindow : FiguraExtras.windows) {
            if (detachedWindow.closeIfRequested()) {
                toRemove.add(detachedWindow);
                continue;
            }

            if (!(doRender || GLFW.glfwGetWindowAttrib(detachedWindow.window.window.getWindow(), GLFW.GLFW_FOCUSED) > 0)) {
                continue;
            }

            profiler.push("Setup Rendering");
            detachedWindow.window.renderTarget.bindWrite(true);
            RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT | GlConst.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            detachedWindow.window.setupTransforms();
            GuiGraphics guiGraphics = new GuiGraphics((Minecraft) (Object) this, this.renderBuffers().bufferSource());


            profiler.pop();
            figuraExtrass$withWindow(detachedWindow.window.window, detachedWindow.window.renderTarget, () -> {
                detachedWindow.render(guiGraphics);
            });

            profiler.push("Finishing rendering");
            guiGraphics.flush();

            detachedWindow.window.endTransforms();

            detachedWindow.window.renderTarget.unbindWrite();
            detachedWindow.window.updateDisplay();
            profiler.pop();
        }

        for (DetachedWindow detachedWindow : toRemove) {
            FiguraExtras.windows.remove(detachedWindow);
        }

        profiler.popPush("FiguraExtras Monitor Component rendering");
        monitorHost.update();
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    void b(Screen screen, CallbackInfo ci) {
        if (screenSetter != null) {
            screenSetter.accept(screen);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void c(CallbackInfo ci) {
        profiler.push("Other windows");
        ArrayList<DetachedWindow> toRemove = new ArrayList<>(0);
        for (DetachedWindow detachedWindow : FiguraExtras.windows) {
            if (detachedWindow.closeIfRequested()) {
                toRemove.add(detachedWindow);
                continue;
            }
            figuraExtrass$withWindow(detachedWindow.window.window, detachedWindow.window.renderTarget, detachedWindow::tick);
        }
        for (DetachedWindow detachedWindow : toRemove) {
            FiguraExtras.windows.remove(detachedWindow);
        }
        profiler.pop();
    }

    public WindowContentPopOutHost figuraExtrass$getContentPopOutHost() {
        return host;
    }

    @Override
    public MonitorContentPopOutHost figuraExtrass$getMonitorPopUpHost() {
        return monitorHost;
    }
}
