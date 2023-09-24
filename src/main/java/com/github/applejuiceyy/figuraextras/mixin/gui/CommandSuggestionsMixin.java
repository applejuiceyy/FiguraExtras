package com.github.applejuiceyy.figuraextras.mixin.gui;

import com.github.applejuiceyy.figuraextras.ducks.CommandSuggestionsAccess;
import com.github.applejuiceyy.figuraextras.ducks.EventsAPIAccess;
import com.github.applejuiceyy.figuraextras.lua.DelayedResponse;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.lua.api.event.LuaEvent;
import org.figuramc.figura.utils.ColorUtils;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin implements CommandSuggestionsAccess {
    @Unique
    boolean shouldShowBadges = false;
    @Unique
    int cachedEnlargement = -1;
    @Unique
    boolean useFiguraSuggester = false;
    @Unique
    CompletableFuture<SuggestionBehaviour> currentBehaviour;


    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Final
    @Shadow
    private List<FormattedCharSequence> commandUsage;
    @Shadow
    private int commandUsageWidth;
    @Shadow
    private boolean allowSuggestions;
    @Shadow
    boolean keepSuggestions;
    @Final
    @Shadow
    private
    Screen screen;

    @Shadow
    private int commandUsagePosition;

    @Shadow
    @Final
    EditBox input;

    @Shadow
    @Final
    Font font;

    @Shadow
    public abstract void updateCommandInfo();

    @Unique
    private Component getLoadingText() {
        MutableComponent load = Component.empty();

        MutableComponent badge = Component.literal(Integer.toHexString(Math.abs(FiguraMod.ticks) % 16));
        badge.setStyle(Style.EMPTY.withColor(ColorUtils.rgbToInt(ColorUtils.Colors.DEFAULT.vec)).withFont(Badges.FONT));
        load.append(badge);
        load.append(" This avatar is loading suggestions");

        return load;
    }

    @Unique
    public CompletableFuture<SuggestionBehaviour> chatAutocompleteEvent(Avatar avatar, String input, int cursor) {
        if (avatar.loaded) {
            LuaEvent ev = ((EventsAPIAccess) avatar.luaRuntime.events).figuraExtras$getAutocompleteEvent();
            if (ev.__len() > 0) {
                DelayedResponse response = new DelayedResponse();
                Varargs execute = avatar.run(ev, avatar.tick, input, cursor + 1, response);

                if (!execute.arg1().isboolean()) {
                    return null;
                }

                try {
                    LuaValue verifying;

                    if ((verifying = execute.arg(2)).isuserdata() && verifying.checkuserdata() == response) {
                        CompletableFuture<CommandSuggestionsAccess.SuggestionBehaviour> handled = response.handle((r, u) -> {
                            try {
                                return CommandSuggestionsAccess.SuggestionBehaviour.parse(r, input);
                            } catch (Exception | StackOverflowError e) {
                                if (avatar.luaRuntime != null)
                                    avatar.luaRuntime.error(e);
                                throw e;
                            }
                        });

                        handled.exceptionally(exc -> {
                            if (exc instanceof CancellationException) {
                                response.cancel(true);
                            }
                            return null;
                        });

                        return handled;
                    } else {
                        return CompletableFuture.completedFuture(CommandSuggestionsAccess.SuggestionBehaviour.parse(verifying, input));
                    }
                } catch (Exception | StackOverflowError e) {
                    if (avatar.luaRuntime != null)
                        avatar.luaRuntime.error(e);
                }
            }
        }

        return null;
    }

    @Unique
    private int getEnlargement() {
        if (cachedEnlargement == -1) {
            cachedEnlargement = this.font.width(Badges.System.DEFAULT.badge.copy().withStyle(Style.EMPTY.withFont(Badges.FONT))) + this.font.width(" ");
        }
        return cachedEnlargement;
    }


    @Override
    public void figuraExtras$setUseFiguraSuggester(boolean use) {
        useFiguraSuggester = use;
    }

    @Override
    public boolean figuraExtras$shouldShowFiguraBadges() {
        return shouldShowBadges;
    }

    @Override
    public Font figuraExtras$getFont() {
        return font;
    }


    @SuppressWarnings("InvalidInjectorMethodSignature")  // the plugin keeps telling that it's wrong for some reason
    @Inject(
            method = "updateCommandInfo",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/client/gui/components/EditBox;getCursorPosition()I",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void useFigura(CallbackInfo ci, String string, StringReader stringReader, boolean bl2, int i) {
        if (!useFiguraSuggester || keepSuggestions) {
            return;
        }

        shouldShowBadges = false;

        // TODO
        if (false) {
            return;
        }

        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null || avatar.luaRuntime == null)
            return;

        if (currentBehaviour != null) {
            currentBehaviour.cancel(true);
        }
        currentBehaviour = chatAutocompleteEvent(avatar, string, i);
        if (currentBehaviour == null) {
            return;
        }

        pendingSuggestions = currentBehaviour.thenApply(behave -> {
            commandUsageWidth = screen.width;
            commandUsagePosition = 0;
            commandUsage.clear();

            // TODO
            shouldShowBadges = true;

            Suggestions suggestions;

            if (behave instanceof AcceptBehaviour accepting) {
                suggestions = accepting.suggest();

                if (suggestions.isEmpty()) {
                    commandUsage.add(FormattedCharSequence.forward("This avatar did not provide any suggestion", Style.EMPTY));
                }
            } else {
                String usage;
                suggestions = Suggestions.merge("", Collections.emptyList());
                if (behave instanceof HintBehaviour hint) {
                    usage = hint.hint();
                    commandUsagePosition = Mth.clamp(input.getScreenX(hint.pos()), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - i);
                    commandUsageWidth = font.width(usage);
                } else if (behave instanceof RejectBehaviour reject) {
                    usage = reject.err();
                } else {
                    throw new RuntimeException("Unexpected " + behave.getClass().getName());
                }

                if (shouldShowBadges) {
                    MutableComponent component = Component.empty();
                    MutableComponent badge = Badges.System.DEFAULT.badge.copy();
                    badge.setStyle(Style.EMPTY.withColor(ColorUtils.rgbToInt(ColorUtils.Colors.DEFAULT.vec)).withFont(Badges.FONT));
                    component.append(badge);
                    component.append(" ");
                    component.append(usage);

                    commandUsage.add(component.getVisualOrderText());
                    commandUsageWidth += getEnlargement();
                } else {
                    commandUsage.add(FormattedCharSequence.forward(usage, Style.EMPTY));
                }
            }

            if (commandUsage.isEmpty()) {
                commandUsageWidth = 0;
            }

            return suggestions;
        });

        pendingSuggestions.thenRun(() -> {
            if (allowSuggestions && Minecraft.getInstance().options.autoSuggestions().get()) {
                ((CommandSuggestions) (Object) this).showSuggestions(false);
            }
        });

        currentBehaviour.exceptionally(exc -> {
            useFiguraSuggester = false;
            try {
                updateCommandInfo();
            } finally {
                useFiguraSuggester = true;
            }
            return null;
        });

        ci.cancel();
    }

    @ModifyArg(
            method = "showSuggestions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/CommandSuggestions$SuggestionsList;<init>(Lnet/minecraft/client/gui/components/CommandSuggestions;IIILjava/util/List;Z)V"
            ),
            index = 3
    )
    public int enlargeToFitBadge(int v) {
        if (!shouldShowBadges) {
            return v;
        }
        return v + getEnlargement();
    }

    @Inject(
            method = "renderUsage",
            at = @At(
                    value = "HEAD"
            )
    )
    public void animateSpinner(GuiGraphics graphics, CallbackInfo ci) {
        if (currentBehaviour != null && !currentBehaviour.isDone()) {
            commandUsage.clear();
            commandUsage.add(getLoadingText().getVisualOrderText());
        }
    }
}
