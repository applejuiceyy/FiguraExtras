package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.compatibility.FabricScreenEventsCompat;
import com.github.applejuiceyy.figuraextras.ducks.MinecraftAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

public abstract class ScreenContainer {
    private final Supplier<Screen> defaultScreen;
    private Screen currentScreen;
    private final FabricScreenEventsCompat fabricCompat = new FabricScreenEventsCompat(this);

    public ScreenContainer(Supplier<Screen> defaultScreen) {
        this.defaultScreen = defaultScreen;
        setScreen(null);
    }

    public void setScreen(Screen screen) {
        withContext(() -> {
            if (currentScreen != null) {
                currentScreen.removed();
            }

            if (screen == null) {
                currentScreen = defaultScreen.get();
            } else {
                currentScreen = screen;
            }

            currentScreen.added();

            currentScreen.init(Minecraft.getInstance(), getInnerWidth(), getInnerHeight());

            newScreen();
        });
    }

    public void dispose() {
        currentScreen.removed();
    }

    public Screen getScreen() {
        return currentScreen;
    }

    public void doEvent(String methodName, Object... args) {
        fabricCompat.invokeFabric(methodName, args);
    }

    public <T> T withContext(Supplier<T> running) {
        return ((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$withSetScreen(this::setScreen, () -> _withContext(running));
    }

    public void withContext(Runnable running) {
        withContext(() -> {
            running.run();
            return null;
        });
    }

    public abstract int getInnerWidth();

    public abstract int getInnerHeight();

    protected abstract <T> T _withContext(Supplier<T> running);

    protected abstract void newScreen();
}
