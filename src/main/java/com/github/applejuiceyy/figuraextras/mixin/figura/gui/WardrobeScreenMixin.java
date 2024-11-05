package com.github.applejuiceyy.figuraextras.mixin.figura.gui;

import com.github.applejuiceyy.figuraextras.ducks.AvatarListAccess;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.avatar.main.MainAvatarsView;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.gui.screens.AbstractPanelScreen;
import org.figuramc.figura.gui.screens.WardrobeScreen;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.SearchBar;
import org.figuramc.figura.gui.widgets.StatusWidget;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WardrobeScreen.class, remap = false)
public abstract class WardrobeScreenMixin extends AbstractPanelScreen {
    @Unique
    int xAnchor = -1;
    @Unique
    private Button BButton;

    private WardrobeScreenMixin(Screen parentScreen, Component title) {
        super(parentScreen, title);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void scootButton(CallbackInfo ci) {
        int x = xAnchor - 16;
        if (!AvatarManager.localUploaded && LocalAvatarLoader.getLoadError() != null) {
            x -= 18;
        }
        BButton.setX(x);
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
        xAnchor = statusWidget.getX();
        BButton = new Button(
                xAnchor - 18,
                statusWidget.getY(),
                14, 14,
                Component.literal("B"),
                null,
                o -> View.newWindow(null, MainAvatarsView::new)
        );


        SearchBar searchBar = ((AvatarListAccess) avatarList).figuraExtrass$getSearchBar();
        setFocused(avatarList);
        avatarList.setFocused(searchBar);
        searchBar.setFocused(searchBar.getField());

        addRenderableWidget(BButton);
    }
}
