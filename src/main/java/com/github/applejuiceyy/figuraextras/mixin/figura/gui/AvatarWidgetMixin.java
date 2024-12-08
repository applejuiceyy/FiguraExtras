package com.github.applejuiceyy.figuraextras.mixin.figura.gui;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.mixin.figura.AvatarWidgetAccessor;
import com.github.applejuiceyy.figuraextras.services.AvatarPostProcessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.avatar.AvatarWidget;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AvatarWidget.class, remap = false)
public class AvatarWidgetMixin {
    @Inject(
            method = "<init>",
            at = @At(
                    value = "TAIL"
            )
    )
    void a(int depth, int width, LocalAvatarFetcher.AvatarPath avatar, AvatarList parent, CallbackInfo ci) {
        ContextMenu menu = ((AvatarWidgetAccessor) this).getContextMenu();
        menu.addAction(Component.literal("Open with " + FiguraExtras.progName.value), null, button -> {
            try {
                String cmd = FiguraExtras.progCmd.value.replace("$folder", avatar.getPath().toString());
                if (Util.getPlatform() == Util.OS.WINDOWS) {
                    Runtime.getRuntime().exec("cmd.exe /c " + cmd);
                } else {
                    Runtime.getRuntime().exec("/bin/sh -c " + cmd);
                }

                FiguraToast.sendToast("On your way");
            } catch (Exception e) {
                FiguraToast.sendToast("Couldn't open", e.getMessage(), FiguraToast.ToastType.ERROR);
            }
        });
    }

    @Inject(method = "lambda$new$0", at = @At("HEAD"), cancellable = true)
    private static void cancelIfPost(LocalAvatarFetcher.AvatarPath avatar, net.minecraft.client.gui.components.Button button, CallbackInfo ci) {
        if (AvatarPostProcessor.INSTANCE.getPostProcessor(avatar.getTheActualPathForThis()) != null) {
            ci.cancel();
        }
    }

    @Mixin(targets = "org/figuramc/figura/gui/widgets/avatar/AvatarWidget$1")
    static abstract class AButtonMixin extends Button {
        @Shadow
        @Final
        LocalAvatarFetcher.AvatarPath val$avatar;
        @Shadow
        @Final
        AvatarWidget val$instance;
        @Unique
        int shakeTick = 0;

        public AButtonMixin(int x, int y, int width, int height, Integer u, Integer v, Integer regionSize, ResourceLocation texture, Integer textureWidth, Integer textureHeight, Component text, Component tooltip, OnPress pressAction) {
            super(x, y, width, height, u, v, regionSize, texture, textureWidth, textureHeight, text, tooltip, pressAction);
        }

        @Inject(method = "renderText", at = @At(value = "TAIL"))
        void wrapIcon(GuiGraphics gui, float delta, CallbackInfo ci) {
            AvatarPostProcessor.PostProcessingInstance postProcessor = AvatarPostProcessor.INSTANCE.getPostProcessor(val$avatar.getTheActualPathForThis());
            if (postProcessor == null) {
                return;
            }
            PoseStack pose = gui.pose();
            pose.pushPose();
            pose.translate(0, 0, 1);
            Button self = this;
            int width = self.getWidth();
            int x = self.getX();
            int y = self.getY();
            int height = self.getHeight();
            gui.fill(x, y, x + width, y + height, 0xaa000000);
            UIHelper.fillOutline(gui, x, y, width, height, 0xFF333333);

            Font font = Minecraft.getInstance().font;
            String text = "Post-processing";
            Component spinner = Component.literal(Integer.toHexString(Math.abs(FiguraMod.ticks) % 16)).withStyle(Style.EMPTY.withFont(Badges.FONT));

            int textSize = font.width(text);
            int spinnerSize = font.width(spinner) * 2;
            int emblemSize = textSize + spinnerSize + 1;

            int halfWidth = width / 2;
            int halfEmblemSize = emblemSize / 2;
            int halfHeight = height / 2;

            gui.fill(x + halfWidth - halfEmblemSize, y + halfHeight - font.lineHeight, x + halfWidth + halfEmblemSize, y + halfHeight + font.lineHeight, 0xFF000000);
            UIHelper.fillOutline(gui, x + halfWidth - halfEmblemSize - 1, y + halfHeight - font.lineHeight - 1, emblemSize + 2, font.lineHeight * 2 + 2, 0xFFFFFFFF);

            gui.fill(x + halfWidth - halfEmblemSize, y + halfHeight + font.lineHeight - 1, x + halfWidth - halfEmblemSize + (int) (emblemSize * postProcessor.getProgress()), y + halfHeight + font.lineHeight, 0xff00ff00);

            pose.pushPose();
            pose.translate(x + width / 2f - emblemSize / 2f, y + height / 2f - font.lineHeight / 2f - 1.5, 0);
            pose.scale(2, 2, 1);

            gui.drawString(font, spinner, 0, 0, -1, false);

            pose.popPose();
            int textPosition = x + halfWidth - halfEmblemSize + spinnerSize + 1;
            gui.drawString(font, text, textPosition, y + halfHeight - font.lineHeight, 0xffffffff);

            int waiting = 0;
            for (Process process : postProcessor.getProcesses()) {
                if (process.isAlive()) waiting++;
            }

            gui.drawString(font, waiting + " left", textPosition, y + halfHeight, 0xffaaaaaa);
            pose.popPose();
        }

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
            float time = FiguraMod.ticks - shakeTick + Minecraft.getInstance().getDeltaFrameTime();
            if (time > 70) {
                setX(val$instance.getX());
            } else {
                setX((int) (val$instance.getX() + Math.cos((time + shakeTick) / 2) * Math.min(time, 1 / (time + 1) * 20) * 3));
            }
            super.render(gui, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (AvatarPostProcessor.INSTANCE.getPostProcessor(val$avatar.getTheActualPathForThis()) != null) {
                shakeTick = FiguraMod.ticks;
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Shadow
        public abstract void setHovered(boolean hovered);
    }
}
