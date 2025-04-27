package com.github.applejuiceyy.figuraextras.smallchange.mixin;

import com.github.applejuiceyy.figuraextras.smallchange.accessors.AvatarListAccess;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.gui.screens.WardrobeScreen;
import org.figuramc.figura.gui.widgets.SearchBar;
import org.figuramc.figura.gui.widgets.StatusWidget;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WardrobeScreen.class, remap = false)
public abstract class WardrobeScreenMixin extends Screen {
    protected WardrobeScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "init",
            at =
            @At(
                    value = "INVOKE",
                    target = "Lorg/figuramc/figura/gui/screens/WardrobeScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
                    ordinal = 9
            ),
            remap = true
    )
    void addButton(CallbackInfo ci, @Local AvatarList avatarList, @Local StatusWidget statusWidget) {
        SearchBar searchBar = ((AvatarListAccess) avatarList).figuraExtras$getSearchBar();
        setFocused(avatarList);
        avatarList.setFocused(searchBar);
        searchBar.setFocused(searchBar.getField());
    }
}
