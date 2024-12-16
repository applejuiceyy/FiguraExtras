package com.github.applejuiceyy.figuraextras.mixin.gui;

import com.github.applejuiceyy.figuraextras.ducks.CommandSuggestionsAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.Font;
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

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public abstract class SuggestionListMixin {
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

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I")
    )
    int renderWithBadge(GuiGraphics instance, Font renderer, String text, int x, int y, int color, Operation<Integer> original, @Local(ordinal = 4) int l) {
        if (!((CommandSuggestionsAccess) field_21615).figuraExtras$shouldShowFiguraBadges()) {
            return original.call(instance, renderer, text, x, y, color);
        }

        Suggestion suggestion = this.suggestionList.get(l + this.offset);

        MutableComponent component = Component.empty();
        MutableComponent badge = Badges.System.DEFAULT.badge.copy();
        badge.setStyle(Style.EMPTY.withColor(ColorUtils.rgbToInt(ColorUtils.Colors.DEFAULT.vec)).withFont(Badges.FONT));
        component.append(badge);
        component.append(" ");
        component.append(suggestion.getText());

        return instance.drawString(((CommandSuggestionsAccess) field_21615).figuraExtras$getFont(), component, rect.getX() + 1, this.rect.getY() + 2 + 12 * l, l + this.offset == this.current ? -256 : -5592406);
    }
}
