package com.github.applejuiceyy.figuraextras.mixin.figura.gui;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.ducks.UserDataAccess;
import com.github.applejuiceyy.figuraextras.mixin.figura.AvatarManagerAccessor;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.avatar.main.MainAvatarsView;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.gui.screens.AbstractPanelScreen;
import org.figuramc.figura.gui.screens.WardrobeScreen;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.utils.TextUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = WardrobeScreen.class, remap = false)
public abstract class WardrobeScreenMixin extends AbstractPanelScreen {
    @Unique
    int BButtonXAnchor = -1;
    @Shadow
    private AvatarInfoWidget infoWidget;
    @Shadow
    private BackendMotdWidget motdWidget;
    @Shadow
    private Button back;
    @Unique
    private Button BButton;
    @Unique
    private Button swapSideButton;
    @Unique
    private Label guestNoticeLabel;

    private WardrobeScreenMixin(Screen parentScreen, Component title) {
        super(parentScreen, title);
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = true)
    void scootButton(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());

        int x = BButtonXAnchor - 16;
        if (!AvatarManager.localUploaded && LocalAvatarLoader.getLoadError() != null) {
            x -= 18;
        }
        BButton.setX(x);


        if (avatar != null) {
            AvatarAccess.Side side = ((AvatarAccess) avatar).figuraExtras$getCurrentSide();
            if (side != null) {

                swapSideButton.setVisible(true);
                swapSideButton.setY(infoWidget.getY() + infoWidget.getHeight() + 4);
                int y = swapSideButton.getY() + swapSideButton.getHeight();
                if (side == AvatarAccess.Side.HOST) {
                    swapSideButton.setMessage(Component.literal("Swap to Guest avatar"));
                    guestNoticeLabel.setVisible(false);
                } else {

                    swapSideButton.setMessage(Component.literal("Swap to Host avatar"));
                    guestNoticeLabel.setVisible(true);
                    guestNoticeLabel.setY(swapSideButton.getY() + swapSideButton.getHeight() + 4);
                    y += 4 + guestNoticeLabel.getHeight();
                }

                if (motdWidget != null) {
                    motdWidget.setY(y + 8);
                    motdWidget.setHeight(back.getY() - (y + 4) - 16);
                }
                return;
            }
        }

        swapSideButton.setVisible(false);
        guestNoticeLabel.setVisible(false);
        if (motdWidget != null) {
            int bottom = infoWidget.getY() + infoWidget.getHeight();
            motdWidget.setY(bottom + 8);
            motdWidget.setHeight(back.getY() - (bottom + 4) - 16);
        }
    }

    @Inject(
            method = "init",
            at =
            @At(
                    value = "INVOKE",
                target = "Lorg/figuramc/figura/gui/screens/WardrobeScreen;addRenderableOnly(Lnet/minecraft/client/gui/components/Renderable;)Lnet/minecraft/client/gui/components/Renderable;",
                ordinal = 2,
                shift = At.Shift.AFTER
            ),
            remap = true
    )
    void addButtons(CallbackInfo ci, @Local AvatarList avatarList, @Local StatusWidget statusWidget) {
        BButtonXAnchor = statusWidget.getX();
        BButton = new Button(
            BButtonXAnchor - 18,
                statusWidget.getY(),
                14, 14,
                Component.literal("B"),
                null,
                o -> View.newWindow(null, MainAvatarsView::new)
        );

        addRenderableWidget(BButton);

        swapSideButton = new Button(
            infoWidget.getX(),
            infoWidget.getY() + infoWidget.getHeight() - 14,
            infoWidget.getWidth(), 14,
            Component.literal("Swap"),
            null,
            WardrobeScreenMixin::swapAvatar
        );

        addRenderableWidget(swapSideButton);

        guestNoticeLabel = new Label(Component.literal("Note: FiguraExtras does not attempt to simulate any behaviour of guest clients").withStyle(ChatFormatting.DARK_GRAY), infoWidget.getX() + infoWidget.getWidth() / 2, 0, infoWidget.getWidth(), true, TextUtils.Alignment.CENTER);

        addRenderableWidget(guestNoticeLabel);
    }

    @Unique
    private static void swapAvatar(net.minecraft.client.gui.components.Button o) {
        AvatarManager.localUploaded = true;

        UUID localPlayerUUID = FiguraMod.getLocalPlayerUUID();
        Avatar avatar = AvatarManager.getAvatarForPlayer(localPlayerUUID);
        CompoundTag nbt = avatar.nbt;
        CompoundTag other = ((AvatarAccess) avatar).figuraExtras$getOtherNbt();
        AvatarAccess.Side side = ((AvatarAccess) avatar).figuraExtras$getCurrentSide();

        UserData user = AvatarManagerAccessor.getLoadedUsers().computeIfAbsent(localPlayerUUID, UserData::new);
        AvatarManager.clearAvatars(localPlayerUUID);

        // prevent the avatar from being fetched back and overriding the one we loaded
        AvatarManagerAccessor.getFetchedUsers().add(localPlayerUUID);

        AvatarAccess.Side newSide = side == AvatarAccess.Side.GUEST ? AvatarAccess.Side.HOST : AvatarAccess.Side.GUEST;
        ((UserDataAccess) user).figuraExtras$setFutureAvatarOtherNbt(
            nbt, newSide
        );

        user.loadAvatar(other);
    }
}
