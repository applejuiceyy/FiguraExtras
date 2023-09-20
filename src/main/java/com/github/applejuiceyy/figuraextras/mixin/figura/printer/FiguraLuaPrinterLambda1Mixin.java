package com.github.applejuiceyy.figuraextras.mixin.figura.printer;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.util.Event;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

@Mixin(targets = "org.figuramc.figura.lua.FiguraLuaPrinter$1")
class FiguraLuaPrinterLambda1Mixin {
    @Shadow
    @Final
    FiguraLuaRuntime val$runtime;

    @Unique
    boolean switchLogOthersBack = false;
    @Unique
    int switchLogLocationBack = 0;

    @Inject(method = "invoke(Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At("HEAD"), remap = false)
    private void e(Varargs args, CallbackInfoReturnable<Varargs> cir) {
        Event<BiConsumer<Component, FiguraLuaPrinterDuck.Kind>> redirect = ((AvatarAccess) val$runtime.owner).figuraExtrass$getChatRedirect();
        FiguraLuaPrinterDuck.currentAvatar = val$runtime.owner;
        FiguraLuaPrinterDuck.currentKind = FiguraLuaPrinterDuck.Kind.PRINT;
        switchLogOthersBack = Configs.LOG_OTHERS.value;
        switchLogLocationBack = Configs.LOG_LOCATION.value;
        if (!Configs.LOG_OTHERS.value && redirect.hasSubscribers()) {
            Configs.LOG_OTHERS.value = true;
            Configs.LOG_LOCATION.value = 0;
        }
    }

    @Inject(method = "invoke(Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At(value = "FIELD", target = "Lorg/figuramc/figura/config/ConfigType$BoolConfig;value:Ljava/lang/Object;", shift = At.Shift.AFTER), remap = false)
    private void c(Varargs args, CallbackInfoReturnable<Varargs> cir) {
        Configs.LOG_OTHERS.value = switchLogOthersBack;
    }

    @Inject(method = "invoke(Lorg/luaj/vm2/Varargs;)Lorg/luaj/vm2/Varargs;", at = @At(value = "TAIL"), remap = false)
    private void h(Varargs args, CallbackInfoReturnable<Varargs> cir) {
        FiguraLuaPrinterDuck.currentAvatar = null;
        FiguraLuaPrinterDuck.currentKind = FiguraLuaPrinterDuck.Kind.OTHER;
        Configs.LOG_LOCATION.value = switchLogLocationBack;
    }
}