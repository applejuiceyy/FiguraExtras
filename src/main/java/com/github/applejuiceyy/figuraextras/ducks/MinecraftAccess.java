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
    VirtualScreen figuraExtrass$getScreenManager();

    <T> T figuraExtrass$withWindow(Window window, RenderTarget target, Supplier<T> runnable);

    default void figuraExtrass$withWindow(Window window, RenderTarget target, Runnable runnable) {
        figuraExtrass$withWindow(window, target, () -> {
            runnable.run();
            return null;
        });
    }

    <T> T figuraExtrass$withSetScreen(Consumer<Screen> setScreen, Supplier<T> runnable);

    default void figuraExtrass$withSetScreen(Consumer<Screen> screen, Runnable runnable) {
        figuraExtrass$withSetScreen(screen, () -> {
            runnable.run();
            return null;
        });
    }

    WindowContentPopOutHost figuraExtrass$getContentPopOutHost();

    MonitorContentPopOutHost figuraExtrass$getMonitorPopUpHost();
}
