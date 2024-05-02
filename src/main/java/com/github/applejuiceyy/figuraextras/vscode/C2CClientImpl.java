package com.github.applejuiceyy.figuraextras.vscode;

import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.figuraextras.vscode.dsp.DebugProtocolServer;
import com.github.applejuiceyy.figuraextras.vscode.ipc.IPCFactory;
import com.github.applejuiceyy.figuraextras.vscode.protocol.C2CClient;
import com.github.applejuiceyy.figuraextras.vscode.protocol.C2CServer;
import com.github.applejuiceyy.figuraextras.vscode.protocol.ClientInformation;
import com.github.applejuiceyy.figuraextras.vscode.protocol.WorldInformation;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.figuramc.figura.FiguraMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class C2CClientImpl implements C2CClient {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:ReceptionistClient");
    public final String receptionistId = UUID.randomUUID().toString();
    private C2CServer server;
    private boolean doSuccession = true;
    private int successionIndex;
    private CompletableFuture<Void> whenLevelLoads;

    @Override
    public void successionIndex(int index) {
        logger.info("Server set succession index: " + index);
        successionIndex = index;
    }

    @Override
    public CompletableFuture<List<WorldInformation>> getClientWorlds() {
        logger.info("Server requested for available client worlds");
        CompletableFuture<List<WorldInformation>> objectCompletableFuture = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> {
            LevelStorageSource.LevelCandidates levelCandidates;
            try {
                levelCandidates = Minecraft.getInstance().getLevelSource().findLevelCandidates();
            } catch (LevelStorageException var3) {
                objectCompletableFuture.complete(List.of());
                return;
            }

            if (levelCandidates.isEmpty()) {
                objectCompletableFuture.complete(List.of());
            } else {
                Minecraft.getInstance()
                        .getLevelSource()
                        .loadLevelSummaries(levelCandidates).exceptionally(throwable -> List.of())
                        .thenApply(summaries ->
                                summaries.stream()
                                        .map(summary -> new WorldInformation(summary.getLevelId(), true))
                                        .collect(Collectors.toList())
                        )
                        .thenAccept(l -> {
                            objectCompletableFuture.complete(l);
                        });
            }
        });
        return objectCompletableFuture;
    }

    @Override
    public CompletableFuture<Void> joinSinglePlayerWorld(String name) {
        Minecraft instance = Minecraft.getInstance();
        if (instance.level != null) {
            return Util.fail(ResponseErrorCode.InvalidRequest, "In a world");
        }
        if (instance.getLevelSource().levelExists(name)) {
            instance.execute(() -> {
                Screen screen = instance.screen;
                instance.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
                instance.createWorldOpenFlows().loadLevel(screen, name);
            });
            whenLevelLoads = new CompletableFuture<>();
            return whenLevelLoads;
        }
        return Util.fail(ResponseErrorCode.InvalidParams, "World doesn't exist");
    }

    @Override
    public CompletableFuture<String> startDAP(String avatarPath) {
        String path = "figuraextras\\direct\\" + UUID.randomUUID();
        ReceptionistServer.logger.info("Starting DAP at " + path);

        Util.thread(() -> {
            try {
                IPCFactory.IPC ipc = IPCFactory.getIPCFactory().createServer(path);
                Tuple<InputStream, OutputStream> connect = ipc.connect();
                DebugProtocolServer.create(ipc);
                DebugProtocolServer instance = DebugProtocolServer.getInstance();
                Launcher<IDebugProtocolClient> serverLauncher = DSPLauncher.createServerLauncher(instance, connect.getA(), connect.getB());
                ReceptionistServer.startWithTermination(instance, connect.getA(), connect.getB(), serverLauncher, () -> {
                    try {
                        ipc.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                assert instance != null;
                instance.connect(serverLauncher.getRemoteProxy());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return CompletableFuture.completedFuture(path);
    }

    public void disallowSuccession(boolean b) {
        doSuccession = !b;
        server.disallowSuccession(b);
    }

    public void updateInformation() {
        C2CClientImpl client = ReceptionistServer.getOrCreateOrConnect();
        WorldInformation info = null;
        if (Minecraft.getInstance().level != null) {
            if (whenLevelLoads != null) {
                whenLevelLoads.complete(null);
                whenLevelLoads = null;
            }
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

        client.getServer().updateInfo(
                new ClientInformation(
                        SharedConstants.getCurrentVersion().getName(),
                        Minecraft.getInstance().gameDirectory.getPath(),
                        FiguraMod.getFiguraDirectory().toString(),
                        receptionistId,
                        DebugProtocolServer.getInstance() != null,
                        info
                )
        );
    }

    public C2CServer getServer() {
        return server;
    }

    public void setServer(C2CServer server) {
        this.server = server;
    }

    @Override
    public void onDisconnect() {
        logger.info("Host disconnected");
        if (doSuccession) {
            new Thread(() -> {
                try {
                    Thread.sleep(successionIndex * 1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ReceptionistServer.getOrCreateOrConnect();
            }).start();
        }
    }
}
