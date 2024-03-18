package com.github.applejuiceyy.figuraextras.mixin.figura;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.AvatarListAccess;
import com.github.applejuiceyy.figuraextras.screen.TestScreen;
import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.gui.screens.AbstractPanelScreen;
import org.figuramc.figura.gui.screens.WardrobeScreen;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = WardrobeScreen.class, remap = false)
public abstract class WardrobeScreenMixin extends AbstractPanelScreen {
    @Shadow
    public abstract void tick();

    private WardrobeScreenMixin(Screen parentScreen, Component title) {
        super(parentScreen, title);
    }

    @Inject(
            method = "init",
            at =
            @At(
                    value = "INVOKE",
                    target = "Lorg/figuramc/figura/gui/screens/WardrobeScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
                    ordinal = 9
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = true
    )
    void c(CallbackInfo ci, Minecraft minecraft, int middle, int panels, int modelBgSize, AvatarList avatarList, int entitySize, int entityX, int entityY, EntityPreview entity, int buttX, int buttY, StatusWidget statusWidget, MutableComponent versionText, int versionStatus, boolean oldVersion, Label version, int rightSide, Button avatarSettings, Button sounds, Button keybinds) {
        Button newButton = new Button(
                statusWidget.getX() - 16,
                statusWidget.getY(),
                14, 14,
                Component.literal("B"),
                null,
                o -> FiguraExtras.windows.add(new DetachedWindow())
        );

        Button testButton = new Button(
                statusWidget.getX() - 32,
                statusWidget.getY(),
                14, 14,
                Component.literal("V"),
                null,
                o -> Minecraft.getInstance().setScreen(new TestScreen(Component.empty()))
        );


        SearchBar searchBar = ((AvatarListAccess) avatarList).figuraExtrass$getSearchBar();
        setFocused(avatarList);
        avatarList.setFocused(searchBar);
        searchBar.setFocused(searchBar.getField());

        addRenderableWidget(newButton);
        addRenderableWidget(testButton);
    }
}
