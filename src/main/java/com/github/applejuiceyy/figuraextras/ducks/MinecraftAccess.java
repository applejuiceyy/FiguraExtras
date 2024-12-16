package com.github.applejuiceyy.figuraextras.ducks;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.MonitorContentPopOutHost;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.VirtualScreen;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface MinecraftAccess {
    VirtualScreen figuraExtras$getScreenManager();

    default void figuraExtras$withWindow(Window window, RenderTarget target, Runnable runnable) {
        figuraExtras$withWindow(window, target, () -> {
            runnable.run();
            return null;
        });
    }

    <T> T figuraExtras$withWindow(Window window, RenderTarget target, Supplier<T> runnable);

    default void figuraExtras$withSetScreen(Consumer<Screen> screen, Runnable runnable) {
        figuraExtras$withSetScreen(screen, () -> {
            runnable.run();
            return null;
        });
    }

    <T> T figuraExtras$withSetScreen(Consumer<Screen> setScreen, Supplier<T> runnable);

    WindowContentPopOutHost figuraExtras$getContentPopOutHost();

    MonitorContentPopOutHost figuraExtras$getMonitorPopUpHost();
}
