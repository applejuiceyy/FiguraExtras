package com.github.applejuiceyy.figuraextras.mixin.figura.printer;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.statics.FiguraLuaPrinterDuck;
import com.github.applejuiceyy.figuraextras.util.Event;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.LuaTypeManager;
import org.figuramc.figura.utils.TextUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;


@Mixin(value = FiguraLuaPrinter.class, remap = false)
public abstract class FiguraLuaPrinterMixin {

    @Shadow
    private static MutableComponent getPrintText(LuaTypeManager typeManager, LuaValue value, boolean hasTooltip, boolean quoteStrings) {
        return null;
    }

    @Inject(method = "sendLuaChatMessage", at = @At("HEAD"), cancellable = true)
    private static void cheese(Component message, CallbackInfo ci) {
        if (FiguraLuaPrinterDuck.currentAvatar == null) {
            return;
        }

        Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> redirect = ((AvatarAccess) FiguraLuaPrinterDuck.currentAvatar).figuraExtrass$getChatRedirect();
        if (redirect.hasSubscribers()) {
            String print = message.getString();
            if (!print.isEmpty()) {
                message = print.endsWith("\n") ? TextUtils.substring(message, 0, print.length() - 1) : message;
            }
            Component finalMessage = message;
            if (redirect.getSink().test(TextUtils.replaceTabs(finalMessage), FiguraLuaPrinterDuck.currentKind)) {
                if (!FiguraLuaPrinterDuck.logOthers && !FiguraMod.isLocal(FiguraLuaPrinterDuck.currentAvatar.owner)) {
                    ci.cancel();
                }
            } else {
                ci.cancel();
            }
            ;
        }
    }

    @Inject(
            method = "sendLuaError",
            at = @At(value = "INVOKE", target = "Ljava/util/LinkedList;offer(Ljava/lang/Object;)Z"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private static void moreCheese(LuaError error, Avatar owner, CallbackInfo ci, String message, MutableComponent component) {
        Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> redirect = ((AvatarAccess) owner).figuraExtrass$getChatRedirect();
        if (redirect.hasSubscribers()) {
            if (!redirect.getSink().test(TextUtils.replaceTabs(component), FiguraLuaPrinterDuck.Kind.ERRORS)) {
                ci.cancel();
            }
        }
    }

    @Unique
    private static Integer switchLogPingsBack = 0;

    @Inject(method = "sendPingMessage", at = @At("HEAD"))
    private static void tamperConfigVariable(Avatar owner, String ping, int size, LuaValue[] args, CallbackInfo ci) {
        Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> redirect = ((AvatarAccess) owner).figuraExtrass$getChatRedirect();
        switchLogPingsBack = Configs.LOG_PINGS.value;
        if (redirect.hasSubscribers()) {
            Configs.LOG_PINGS.value = 2;
        }
    }

    @Inject(method = "sendPingMessage", at = @At(value = "FIELD", target = "Lorg/figuramc/figura/config/Configs;LOG_LOCATION:Lorg/figuramc/figura/config/ConfigType$EnumConfig;"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private static void evenMoreCheese(Avatar owner, String ping, int size, LuaValue[] args, CallbackInfo ci, int config, MutableComponent text) {
        Event<BiPredicate<Component, FiguraLuaPrinterDuck.Kind>> redirect = ((AvatarAccess) owner).figuraExtrass$getChatRedirect();
        Configs.LOG_PINGS.value = switchLogPingsBack;
        if (!redirect.getSink().test(text, FiguraLuaPrinterDuck.Kind.PINGS) || (switchLogPingsBack == 0 || switchLogPingsBack == 1 && !owner.isHost)) {
            ci.cancel();
        }
    }

    @Inject(method = "userdataToText", at = @At(value = "HEAD"), cancellable = true)
    private static void amDead(LuaTypeManager typeManager, LuaValue value, int depth, int indent, boolean hasTooltip, CallbackInfoReturnable<Component> cir) {
        if (FiguraLuaPrinterDuck.skipUserdataStuff) {
            cir.setReturnValue(getPrintText(typeManager, value, hasTooltip, true));
        }
    }
}
