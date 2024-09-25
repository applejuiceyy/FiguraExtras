package com.github.applejuiceyy.figuraextras;


import com.github.applejuiceyy.figuraextras.ducks.SoundEngineAccess;
import com.github.applejuiceyy.figuraextras.ipc.IPCManager;
import com.github.applejuiceyy.figuraextras.ipc.ReceptionistServer;
import com.github.applejuiceyy.figuraextras.ipc.backend.ReceptionistServerBackend;
import com.github.applejuiceyy.figuraextras.ipc.dsp.DebugProtocolServer;
import com.github.applejuiceyy.figuraextras.ipc.protocol.ClientInformation;
import com.github.applejuiceyy.figuraextras.ipc.protocol.WorldInformation;
import com.github.applejuiceyy.figuraextras.views.TabView;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.backend.AvatarInstance;
import com.github.applejuiceyy.figuraextras.views.backend.PlayerInstance;
import com.github.applejuiceyy.figuraextras.window.DetachedWindow;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.ducks.ChannelHandleAccessor;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class FiguraExtras implements ClientModInitializer {
    public static final ConfigType.StringConfig progName;
    public static final ConfigType.BoolConfig disableServerToasts;
    public static final ConfigType.BoolConfig disableCachedRendering;
    public static final ConfigType.StringConfig progCmd;
    public static final ConfigType.IntConfig prepInstructionCount;
    public static final ConfigType.EnumConfig signAvatars;
    private static final ConfigType.BoolConfig localBackend;
    private static final ConfigType.Category category;

    public static ArrayList<DetachedWindow> windows = new ArrayList<>();
    public static Object2IntArrayMap<UUID> showSoundPositions = new Object2IntArrayMap<>();
    public static Logger logger = LogUtils.getLogger();

    private static Path globalMinecraftDirectory;
    private static Path figuraExtrasDirectory;

    private static UUID instanceUUID;
    public static SymmetricSigner avatarSigner;



    static {

        try {
            Class.forName("org.figuramc.figura.config.Configs");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        category = new ConfigType.Category("FiguraExtras");

        prepInstructionCount = new ConfigType.IntConfig("prep_instruction_count", category, 1000000);
        progName = new ConfigType.StringConfig("prog_name", category, "Visual Studio Code");
        progCmd = new ConfigType.StringConfig("prog_cmd", category, "code \"$folder\"");
        disableServerToasts = new ConfigType.BoolConfig("disable_server_toasts", category, false);
        disableCachedRendering = new ConfigType.BoolConfig("disable_cached_rendering", category, false);
        signAvatars = new ConfigType.EnumConfig("sign_avatars", category, 2, 4) {{
            enumList = List.of(
                    Component.literal("Skip Signing"),
                    Component.literal("Ignore Signing"),
                    Component.literal("Warn Incompatible Signatures"),
                    Component.literal("Reject Incompatible Signatures")
            );
            enumTooltip = List.of(
                    Component.literal("Skip signing entirely"),
                    Component.literal("Skip verifying signatures but still sign"),
                    Component.literal("Emit a warning when a signature isn't matching"),
                    Component.literal("reject when a signature isn't matching")
            );
        }};

        ConfigType.ButtonConfig kofi = new ConfigType.ButtonConfig("kofi", category, () -> Util.getPlatform().openUri("https://ko-fi.com/theapplejuice"));
        kofi.name = Component.literal("Huh? Kofi? Is that some kind of lettuce?").withStyle(ChatFormatting.GREEN);

        localBackend = new ConfigType.BoolConfig("divert_backend", category, false) {
            @Override
            public void onChange() {
                IPCManager.INSTANCE.divertBackend.divertBackend(value);
            }
        };
        localBackend.name = Component.literal("Use local backend");

        new ConfigType.ButtonConfig("other_test", category, () -> {
            if (!IPCManager.INSTANCE.isHost()) {
                FiguraToast.sendToast("Not the owner of the backend");
            }

            ReceptionistServer server = IPCManager.INSTANCE.getReceptionistServer();
            View.newWindow(server, (ctx, ap) -> {
                TabView<ReceptionistServer> tabView = new TabView<>(ctx, ap);
                ReceptionistServerBackend backend = server.getBackend();
                tabView.add("Players", View.differential(
                        c -> c.getValue().getUsers(),
                        ReceptionistServerBackend.BackendUser::getUuid,
                        PlayerInstance::new
                ), backend);
                tabView.add("Avatars", View.differential(
                        c -> c.getValue().getAvatars(),
                        avatar -> avatar.getOwner() + "-" + avatar.getId(),
                        AvatarInstance::new
                ), backend);
                return tabView;
            });
        }).name = Component.literal("Open local backend view");

        category.name = Component.literal("FiguraExtras");
        prepInstructionCount.name = Component.literal("Instruction limit for preprocessor");
        progName.name = Component.literal("Open With Program Name");
        progCmd.name = Component.literal("Open With Program Command");
        disableServerToasts.name = Component.literal("Disable Backend Toasts");
        disableCachedRendering.name = Component.literal("Disable Cached Rendering");
        signAvatars.name = Component.literal("Sign Avatars");
    }

    public static void sendBrandedMessage(Component text) {
        sendBrandedMessage("FiguraExtras", text);
    }

    public static void sendBrandedMessage(String header, Component text) {
        sendBrandedMessage(header, ChatFormatting.BLUE, text);
    }

    public static void sendBrandedMessage(String header, ChatFormatting formatting, Component text) {
        sendBrandedMessage(header, style -> style.withColor(formatting), text);
    }

    public static void sendBrandedMessage(String header, UnaryOperator<Style> style, Component text) {
        Minecraft.getInstance().execute(() -> FiguraMod.sendChatMessage(
                Component.literal("[" + header + "] ")
                        .withStyle(style)
                        .append(text)
        ));
    }

    public static void sendBrandedMessage(String text) {
        sendBrandedMessage(Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    public static void sendBrandedMessage(String header, String text) {
        sendBrandedMessage(header, Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    public static void sendBrandedMessage(String header, ChatFormatting formatting, String text) {
        sendBrandedMessage(header, formatting, Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    public static void sendBrandedMessage(String header, UnaryOperator<Style> style, String text) {
        sendBrandedMessage(header, style, Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    public static UUID getInstanceUUID() {
        return instanceUUID;
    }

    public static Path getFiguraExtrasDirectory() {
        return figuraExtrasDirectory;
    }

    public static Path getGlobalMinecraftDirectory() {
        return globalMinecraftDirectory;
    }

    public static void updateInformation() {
        WorldInformation info = getWorldInformation();

        UUID profileId = Minecraft.getInstance().getUser().getProfileId();
        IPCManager.INSTANCE.setCurrentInformation(
                new ClientInformation(
                        SharedConstants.getCurrentVersion().getName(),
                        Minecraft.getInstance().gameDirectory.getPath(),
                        FiguraMod.getFiguraDirectory().toString(),
                        FiguraExtras.getInstanceUUID().toString(),
                        UUIDUtil.createOfflinePlayerUUID(Minecraft.getInstance().getUser().getName()).toString(),
                        profileId == null ? null : profileId.toString(),
                        DebugProtocolServer.getInstance() != null,
                        info
                )
        );
    }

    private static @Nullable WorldInformation getWorldInformation() {
        WorldInformation info = null;
        if (Minecraft.getInstance().level != null) {
            IntegratedServer iServer = Minecraft.getInstance().getSingleplayerServer();
            if (iServer != null) {
                info = new WorldInformation(iServer.getWorldData().getLevelName(), true);
            } else {
                ServerData mServer = Minecraft.getInstance().getCurrentServer();
                if (mServer != null) {
                    info = new WorldInformation(mServer.name, true);
                }
            }
        }
        return info;
    }

    @Override
    public void onInitializeClient() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        String s = switch (Util.getPlatform()) {
            case WINDOWS -> System.getenv("APPDATA") + "/.minecraft";
            case OSX -> System.getProperty("user.home") + "/Library/Application Support/minecraft";
            default -> System.getProperty("user.home") + "/.minecraft";
        };
        globalMinecraftDirectory = Path.of(s, "figura_extras", "global");
        try {
            try {
                figuraExtrasDirectory = gameDir.resolve("figura_extras");
                Files.createDirectories(figuraExtrasDirectory);
            } catch (FileAlreadyExistsException ignored) {
            }

            try {
                Files.createDirectories(globalMinecraftDirectory);
            } catch (FileAlreadyExistsException ignored) {
            }

            Path file = figuraExtrasDirectory.resolve("id");
            if (Files.exists(file)) {
                try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file))) {
                    char[] uuid = new char[36];
                    if (reader.read(uuid) == 36 && reader.read() == -1) {
                        instanceUUID = UUID.fromString(String.valueOf(uuid));
                    }
                }
            }
            if (instanceUUID == null) {
                instanceUUID = UUID.randomUUID();
                Files.write(file, instanceUUID.toString().getBytes());
            }

            SecretKey key = null;

            file = globalMinecraftDirectory.resolve("avatarkey");
            Mac mac = Mac.getInstance("HmacSHA256");
            reading:
            if (Files.exists(file)) {
                logger.info("Sourcing generated secret key");
                SecretKeySpec hmacSHA256 = new SecretKeySpec(Files.readAllBytes(file), "HmacSHA256");

                try {
                    mac.init(hmacSHA256);
                } catch (InvalidKeyException e) {
                    logger.warn("Deleting key file because apparently it's invalid");
                    Files.delete(file);
                    break reading;
                }

                key = hmacSHA256;
            }

            if (key == null) {
                logger.info("Generating a new secret key");
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA256");
                generator.init(1024 * 8);
                key = generator.generateKey();
                try {
                    mac.init(key);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
                Files.write(file, key.getEncoded());
            }
            avatarSigner = new SymmetricSigner() {
                @Override
                public byte[] sign(byte[] bytes) {
                    return mac.doFinal(bytes);
                }

                @Override
                public boolean verify(byte[] message, byte[] signature) {
                    return Arrays.equals(sign(message), signature);
                }
            };
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        updateInformation();
        IPCManager.INSTANCE.enableConnections(true);
        IPCManager.INSTANCE.divertBackend.divertBackend(localBackend.value);

        HudRenderCallback.EVENT.register((p, o) -> {
            if (DebugProtocolServer.getInstance() != null) {
                MutableComponent text = Component.literal("debugging");
                p.drawString(Minecraft.getInstance().font, text, Minecraft.getInstance().getWindow().getGuiScaledWidth() - Minecraft.getInstance().font.width(text), Minecraft.getInstance().getWindow().getGuiScaledHeight() - 9, 0xffffffff);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            IPCManager.INSTANCE.closeEverything();
            com.github.applejuiceyy.figuraextras.util.Util.closeMultiple(DebugProtocolServer::close);
        });

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

    public interface SymmetricSigner {
        byte[] sign(byte[] bytes);

        boolean verify(byte[] message, byte[] signature);
    }
}
