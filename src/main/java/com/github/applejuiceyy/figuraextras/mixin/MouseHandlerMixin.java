package com.github.applejuiceyy.figuraextras.mixin;

import com.github.applejuiceyy.figuraextras.ducks.MinecraftAccess;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private int activeButton;
    @Shadow
    private double xpos;
    @Shadow
    private double ypos;

    @Shadow
    public abstract boolean isMouseGrabbed();

    @SuppressWarnings("InvalidInjectorMethodSignature") // it's drunk again
    @Inject(
            method = "onMove", at = @At(
            ordinal = 1,
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    void a(long window, double x, double y, CallbackInfo ci, Screen screen, double d, double e, double f, double g) {
        if (!isMouseGrabbed() &&
                ((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$getContentPopOutHost().onMouseDrag(d, e, activeButton, f, g)) {
            xpos = x;
            ypos = y;
            ci.cancel();
        }
    }

    @SuppressWarnings("InvalidInjectorMethodSignature") // it's drunk again
    @Inject(
            method = "onPress", at = @At(
            ordinal = 2,
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    void a(long window, int button, int action, int modifiers, CallbackInfo ci, boolean bl, int i, boolean[] bls, double d, double e) {
        WindowContentPopOutHost o = ((MinecraftAccess) Minecraft.getInstance()).figuraExtrass$getContentPopOutHost();

        if (bl) {
            if (o.onMouseDown(d, e, i)) {
                ci.cancel();
            }
        } else {
            if (o.onMouseRelease(d, e, i)) {
                ci.cancel();
            }
        }

    }
}
