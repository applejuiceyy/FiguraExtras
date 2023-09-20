package com.github.applejuiceyy.figuraextras.mixin.window;

import com.github.applejuiceyy.figuraextras.ducks.WindowAccess;
import com.github.applejuiceyy.figuraextras.ducks.WindowDuck;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin implements WindowAccess {
    @Unique
    boolean terminateGLFW = true;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"),
            index = 4)
    long a(long share) {
        //noinspection ConstantValue
        return Minecraft.getInstance().getWindow() == null ? share : Minecraft.getInstance().getWindow().getWindow();
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwTerminate()V"), cancellable = true)
    void e(CallbackInfo ci) {
        if (!terminateGLFW) {
            ci.cancel();
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    void i(WindowEventHandler eventHandler, ScreenManager monitorTracker, DisplayData settings, String videoMode, String title, CallbackInfo ci) {
        if (WindowDuck.hints != null) {
            WindowDuck.hints.run();
        }
    }

    @Override
    public void figuraExtrass$setShouldTerminateGLFWOnExit(boolean terminate) {
        terminateGLFW = terminate;
    }
}
