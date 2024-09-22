package com.github.applejuiceyy.figuraextras.ipc;

import com.github.applejuiceyy.figuraextras.ipc.dsp.DebugProtocolServer;
import com.github.applejuiceyy.figuraextras.ipc.protocol.C2CClient;
import com.github.applejuiceyy.figuraextras.ipc.protocol.C2CServer;
import com.github.applejuiceyy.figuraextras.ipc.protocol.WorldInformation;
import com.github.applejuiceyy.figuraextras.ipc.underlying.IPCFactory;
import com.github.applejuiceyy.figuraextras.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.figuramc.figura.backend2.websocket.S2CMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class C2CClientImpl implements C2CClient {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:ReceptionistClient");
    private final Runnable disconnect;
    private C2CServer server;
    private CompletableFuture<Void> whenLevelLoads;

    public C2CClientImpl(Runnable disconnect) {
        this.disconnect = disconnect;
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
                        .thenAccept(objectCompletableFuture::complete);
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
            CompletableFuture<?> thisFuture;
            thisFuture = whenLevelLoads = new CompletableFuture<>();
            instance.execute(() -> {
                instance.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
                instance.createWorldOpenFlows().checkForBackupAndLoad(name, () -> {
                    thisFuture.completeExceptionally(new Throwable("Level was not loaded"));
                    if (whenLevelLoads == thisFuture) whenLevelLoads = null;
                });
            });
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
                JSONPUtils.startWithTermination(instance, connect.getA(), connect.getB(), serverLauncher, () -> {
                    IPCManager.INSTANCE.closeables.remove(connect.getA());
                    IPCManager.INSTANCE.closeables.remove(connect.getB());
                    IPCManager.INSTANCE.closeables.remove(ipc);
                    try {
                        ipc.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                IPCManager.INSTANCE.closeables.add(connect.getA());
                IPCManager.INSTANCE.closeables.add(connect.getB());
                IPCManager.INSTANCE.closeables.add(ipc);
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

    @Override
    public C2CClientBackend getBackend() {
        return new C2CClientBackend() {
            @Override
            public CompletableFuture<Void> websocketEvent(byte[] bytes) {
                S2CMessageHandler.handle(ByteBuffer.wrap(bytes));
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<?> request(String method, Object parameter) {
                return new CompletableFuture<>();
            }

            @Override
            public void notify(String method, Object parameter) {
            }
        };
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
        disconnect.run();
    }

    public void joinedWorld() {
        if (whenLevelLoads != null) {
            whenLevelLoads.complete(null);
            whenLevelLoads = null;
        }
    }
}
