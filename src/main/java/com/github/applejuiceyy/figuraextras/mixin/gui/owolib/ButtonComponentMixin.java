package com.github.applejuiceyy.figuraextras.mixin.gui.owolib;

import com.github.applejuiceyy.figuraextras.ducks.ButtonComponentAccess;
import io.wispforest.owo.ui.component.ButtonComponent;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonComponent.class)
public class ButtonComponentMixin implements ButtonComponentAccess {
    boolean flash = false;

    @Inject(method = "renderWidget", at = @At("TAIL"))
    void b(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ButtonComponent t = ((ButtonComponent) (Object) this);
        if (flash) {
            context.renderOutline(t.x(), t.y(), t.width(), t.height(), 0xffff0000);
        }
        flash = false;
    }

    @Override
    public void figuraExtrass$setFlash() {
        flash = true;
    }
}
