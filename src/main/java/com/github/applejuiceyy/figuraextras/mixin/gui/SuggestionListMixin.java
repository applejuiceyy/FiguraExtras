package com.github.applejuiceyy.figuraextras.mixin.gui;

import com.github.applejuiceyy.figuraextras.ducks.CommandSuggestionsAccess;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.utils.ColorUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public class SuggestionListMixin {
    @Shadow
    @Final
    CommandSuggestions field_21615;
    @Shadow
    @Final
    private Rect2i rect;
    @Shadow
    private int offset;
    @Shadow
    private int current;
    @Shadow
    @Final
    private List<Suggestion> suggestionList;

    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"),
            index = 1
    )
    String figura_jankCancelRender(String text) {
        if (!((CommandSuggestionsAccess) field_21615).figuraExtras$shouldShowFiguraBadges()) {
            return text;
        }
        // jank-cancel
        return "";
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    void figura_renderOwn(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci, int i, int j, boolean bl, boolean bl2, boolean bl3, boolean bl4, boolean bl5, int l) {
        if (!((CommandSuggestionsAccess) field_21615).figuraExtras$shouldShowFiguraBadges()) {
            return;
        }

        // jank-redo
        Suggestion suggestion = this.suggestionList.get(l + this.offset);

        MutableComponent component = Component.empty();
        MutableComponent badge = Badges.System.DEFAULT.badge.copy();
        badge.setStyle(Style.EMPTY.withColor(ColorUtils.rgbToInt(ColorUtils.Colors.DEFAULT.vec)).withFont(Badges.FONT));
        component.append(badge);
        component.append(" ");
        component.append(suggestion.getText());

        graphics.drawString(((CommandSuggestionsAccess) field_21615).figuraExtras$getFont(), component, rect.getX() + 1, this.rect.getY() + 2 + 12 * l, l + this.offset == this.current ? -256 : -5592406);
    }
}
