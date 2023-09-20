package com.github.applejuiceyy.figuraextras.mixin.render;

import com.github.applejuiceyy.figuraextras.ducks.ButtonComponentAccess;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public class AbstractWidgetMixin {
    @Inject(method = "setMessage", at = @At("HEAD"))
    void a(Component message, CallbackInfo ci) {
        if (this instanceof ButtonComponentAccess a) {
            a.figuraExtrass$setFlash();
        }
    }
}
