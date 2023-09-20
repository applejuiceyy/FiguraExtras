package com.github.applejuiceyy.figuraextras.mixin.gui;

import com.github.applejuiceyy.figuraextras.ducks.CommandSuggestionsAccess;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    CommandSuggestions commandSuggestions;

    @Inject(at = @At("TAIL"), method = "init")
    private void init(CallbackInfo ci) {
        ((CommandSuggestionsAccess) commandSuggestions).figuraExtras$setUseFiguraSuggester(true);
    }
}
