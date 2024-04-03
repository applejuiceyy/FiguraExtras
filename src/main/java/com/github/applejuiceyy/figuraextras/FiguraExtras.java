package com.github.applejuiceyy.figuraextras;


import com.github.applejuiceyy.figuraextras.ducks.SoundEngineAccess;
import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.ducks.ChannelHandleAccessor;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class FiguraExtras implements ClientModInitializer {
    public static ArrayList<DetachedWindow> windows = new ArrayList<>();

    public static Object2IntArrayMap<UUID> showSoundPositions = new Object2IntArrayMap<>();

    public static final ConfigType.StringConfig progName;
    public static final ConfigType.BoolConfig disableServerToasts;
    public static final ConfigType.BoolConfig disableCachedRendering;
    public static final ConfigType.StringConfig progCmd;
    private static final ConfigType.Category category;

    static {
        try {
            Class.forName("org.figuramc.figura.config.Configs");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        category = new ConfigType.Category("FiguraExtras");
        progName = new ConfigType.StringConfig("prog_name", category, "Visual Studio Code");
        progCmd = new ConfigType.StringConfig("prog_cmd", category, "code \"$folder\"");
        disableServerToasts = new ConfigType.BoolConfig("disable_server_toasts", category, false);
        disableCachedRendering = new ConfigType.BoolConfig("disable_cached_rendering", category, false);


        category.name = Component.literal("FiguraExtras");
        progName.name = Component.literal("Open With Program Name");
        progCmd.name = Component.literal("Open With Program Command");
        disableServerToasts.name = Component.literal("Disable Backend Toasts");
        disableCachedRendering.name = Component.literal("Disable Cached Rendering");
    }

    public static Logger logger = Logger.getLogger("FiguraExtras");

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (showSoundPositions.isEmpty()) return;
            Style style = Badges.System.SOUND.badge.getStyle();
            Component speakers = Badges.System.SOUND.badge.copy().withStyle(style.withFont(Badges.FONT));
            PoseStack poseStack = ctx.matrixStack();
            for (LuaSound sound : ((SoundEngineAccess) SoundAPI.getSoundEngine()).figuraExtrass$getFiguraHandles()) {
                ChannelHandleAccessor accessor = (ChannelHandleAccessor) sound.getHandle();
                if (sound.isPlaying() && accessor != null && showSoundPositions.containsKey(accessor.getOwner())) {
                    poseStack.pushPose();
                    FiguraVec3 pos = sound.getPos();
                    Vec3 position = ctx.camera().getPosition();
                    poseStack.translate(-position.x + pos.x, -position.y + pos.y, -position.z + pos.z);
                    poseStack.mulPose(ctx.camera().rotation());
                    poseStack.scale(-0.1f, -0.1f, 0.1f);

                    Matrix4f matrix4f = poseStack.last().pose();
                    BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                    bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                    bufferBuilder.vertex(matrix4f, -1, -1, 0).color(0xffff0000).endVertex();
                    bufferBuilder.vertex(matrix4f, -1, 1, 0).color(0xffff0000).endVertex();
                    bufferBuilder.vertex(matrix4f, 1, 1, 0).color(0xffff0000).endVertex();
                    bufferBuilder.vertex(matrix4f, 1, -1, 0).color(0xffff0000).endVertex();

                    RenderType gui = RenderType.gui();
                    BufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();
                    gui.setupRenderState();
                    RenderSystem.disableDepthTest();
                    BufferUploader.drawWithShader(renderedBuffer);
                    gui.clearRenderState();

                    poseStack.scale(0.2f, 0.2f, 0.2f);

                    Minecraft.getInstance().font.drawInBatch(
                            speakers,
                            -4.5f, -3f,
                            0xffffffff,
                            false,
                            matrix4f,
                            Minecraft.getInstance().renderBuffers().bufferSource(),
                            Font.DisplayMode.SEE_THROUGH,
                            0x000000,
                            LightTexture.pack(15, 15)
                    );

                    poseStack.popPose();
                }
            }
        });
    }
}
