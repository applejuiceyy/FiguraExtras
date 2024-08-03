package com.github.applejuiceyy.figuraextras.ipc;

import com.github.applejuiceyy.figuraextras.ipc.backend.ReceptionistServerBackend;
import com.github.applejuiceyy.figuraextras.ipc.protocol.*;
import com.github.applejuiceyy.figuraextras.ipc.underlying.IPCFactory;
import com.github.applejuiceyy.figuraextras.util.Util;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Tuple;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReceptionistServer {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:ReceptionistServer");
    private static C2CClientImpl currentClient;
    private static ReceptionistServer currentReceptionistServer;
    private static IPCFactory.IPC receptionistPipeServer;
    private static IPCFactory.IPC otherClientServer;

    ArrayList<ServerClientInterface> eligibleToSuccession = new ArrayList<>();
    ArrayList<ServerClientInterface> allClients = new ArrayList<>();
    ArrayList<ReceptionistVSCInterface> vscInterfaces = new ArrayList<>();
    ReceptionistServerBackend backend = new ReceptionistServerBackend();

    public ReceptionistServer() {

    }

    @Nullable
    public static ReceptionistServer getCurrentReceptionistServer() {
        return currentReceptionistServer;
    }

    public static C2CClientImpl getOrCreateOrConnect() {
        if (currentClient != null) {
            return currentClient;
        }

        String RECEPTIONIST_PATH = "figuraextras\\receptionist";
        String OTHER2HOST_PATH = "figuraextras\\C2CServer";

        IPCFactory ipcFactory = IPCFactory.getIPCFactory();
        boolean succession = true;
        if (!ipcFactory.exists(OTHER2HOST_PATH)) {
            succession = false;
            ReceptionistServer receptionistServer = new ReceptionistServer();


            try {
                otherClientServer = ipcFactory.createServer(OTHER2HOST_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                receptionistPipeServer = ipcFactory.createServer(RECEPTIONIST_PATH);
            } catch (IOException e) {
                try {
                    otherClientServer.close();
                } catch (IOException ignored) {
                }
                otherClientServer = null;
                throw new RuntimeException(e);
            }

            otherClientServer.continuousConnect(receptionistServer::otherClientConnection);
            receptionistPipeServer.continuousConnect(receptionistServer::vscodeConnection);

            ReceptionistServer.currentReceptionistServer = receptionistServer;
        }
        try {
            Tuple<InputStream, OutputStream> bundle = ipcFactory.connectAsClient(OTHER2HOST_PATH);
            currentClient = createLauncher(bundle.getA(), bundle.getB());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!succession) {
            currentClient.disallowSuccession(true);
        }
        return currentClient;
    }

    public static void close() throws IOException {
        if (otherClientServer == null) return;
        try {
            otherClientServer.close();
        } catch (IOException e) {
            try {
                receptionistPipeServer.close();
            } catch (IOException ignored) {
                throw new IOException("Failed to close both servers");
            }
            throw e;
        }
        receptionistPipeServer.close();
        currentClient = null;
        currentReceptionistServer = null;
    }

    private static C2CClientImpl createLauncher(InputStream i, OutputStream o) {
        C2CClientImpl client = new C2CClientImpl();
        configureLauncher(client, C2CServer.class, i, o, client::setServer);
        return client;
    }

    public static <T> CompletableFuture<T> listenForCompletion(Future<T> fut) {
        CompletableFuture<T> cfut = new CompletableFuture<>();
        cfut.completeAsync(() -> {
            try {
                return fut.get();
            } catch (InterruptedException | ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof RuntimeException r ? r : new RuntimeException(cause);
            }
        });
        return cfut;
    }

    public static <T> T configureLauncher(Object local, Class<? extends T> remote, InputStream i, OutputStream o, Consumer<T> backwardsSetter) {
        Launcher<T> launcher = new Launcher.Builder<T>()
                .setLocalService(local)
                .setRemoteInterface(remote)
                .setInput(i)
                .setOutput(o)
                .configureGson(GsonBuilder::serializeNulls)
                .create();

        T remoteProxy = launcher.getRemoteProxy();
        backwardsSetter.accept(remoteProxy);
        startWithTermination(local, i, o, launcher, () -> {
        });
        return launcher.getRemoteProxy();
    }

    public static <T> void startWithTermination(Object local, InputStream i, OutputStream o, Launcher<T> launcher, Runnable closer) {
        listenForCompletion(launcher.startListening()).handle((a, b) -> {
            if (local instanceof DisconnectAware da) {
                da.onDisconnect();
            }
            Util.closeMultiple(i, o, closer::run);
            return null;
        });
    }

    public void otherClientConnection(InputStream i, OutputStream o) {
        logger.info("Other minecraft client connected");
        ServerClientInterface e = new ServerClientInterface(i, o);
        eligibleToSuccession.add(e);
        allClients.add(e);
        configureLauncher(e, C2CClient.class, i, o, e::setClient);
    }

    public void vscodeConnection(InputStream i, OutputStream o) {
        logger.info("VSCode instance connected");
        ReceptionistVSCInterface e = new ReceptionistVSCInterface(i, o);
        vscInterfaces.add(e);
        configureLauncher(e, ReceptionistClient.class, i, o, e::setClient);
    }

    public ReceptionistServerBackend getBackend() {
        return backend;
    }

    public class ServerClientInterface implements C2CServer {
        final ReceptionistServerBackendConnection receptionistServerBackendConnection = new ReceptionistServerBackendConnection(this, ReceptionistServer.this);
        C2CClient client;
        @Nullable
        ClientInformation information;
        private boolean successionDisallowed = false;

        public ServerClientInterface(InputStream i, OutputStream o) {
        }

        @Override
        public void disallowSuccession(boolean disallow) {
            logger.info("Minecraft client wished to {} succession", disallow ? "disallow" : "allow");
            if (successionDisallowed != disallow) {
                successionDisallowed = disallow;
                if (disallow) {
                    eligibleToSuccession.remove(this);
                    updateEligibleToSuccession();
                } else {
                    client.successionIndex(eligibleToSuccession.size() - 1);
                    eligibleToSuccession.add(this);
                }
            }
        }

        private void updateEligibleToSuccession() {
            for (int i = 0; i < eligibleToSuccession.size(); i++) {
                ServerClientInterface serverClientInterface = eligibleToSuccession.get(i);
                serverClientInterface.client.successionIndex(i);
            }
        }

        @Override
        public void updateInfo(ClientInformation information) {
            logger.info("Minecraft client wished to update identity: {}", information);
            this.information = information;
        }

        @Override
        public C2CServerBackend getBackend() {
            return receptionistServerBackendConnection;
        }

        public void setClient(C2CClient client) {
            this.client = client;
            client.successionIndex(eligibleToSuccession.indexOf(this));
        }

        @Override
        public void onDisconnect() {
            logger.info("Minecraft client disconnected");
            allClients.remove(this);
            eligibleToSuccession.remove(this);
            updateEligibleToSuccession();
        }

        @Override
        public CompletableFuture<?> request(String method, Object parameter) {
            System.out.println(method);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void notify(String method, Object parameter) {

        }
    }

    class ReceptionistVSCInterface implements com.github.applejuiceyy.figuraextras.ipc.protocol.ReceptionistServer {

        private ReceptionistClient client;

        public ReceptionistVSCInterface(InputStream i, OutputStream o) {

        }

        @Override
        public CompletableFuture<List<ClientInformation>> getClients() {
            logger.info("VSCode requested for client catalogue");
            List<ClientInformation> information = new ArrayList<>();
            for (ServerClientInterface client : allClients) {
                if (client.information != null) {
                    information.add(client.information);
                }
                ;
            }
            return CompletableFuture.completedFuture(information);
        }

        @Override
        public CompletableFuture<List<WorldInformation>> getClientWorlds(String id) {
            logger.info("VSCode requested for available client worlds");
            return relay(id, C2CClient::getClientWorlds);
        }

        @Override
        public CompletableFuture<Void> joinSinglePlayerWorld(String id, String name) {
            return relay(id, c -> c.joinSinglePlayerWorld(name));
        }

        @Override
        public CompletableFuture<String> startDAP(String id, String avatarPath) {
            Optional<ServerClientInterface> first = allClients.stream().filter(client -> client.information != null && client.information.instanceId().equals(id)).findFirst();
            if (first.isEmpty()) {
                return Util.fail(ResponseErrorCode.InvalidParams, "Unknown client by id");
            }
            return relay(id, (a, c) -> {
                assert c.information != null;
                if (c.information.world() == null || c.information.isConnected()) {
                    return Util.fail(ResponseErrorCode.InvalidParams, "Invalid client");
                }

                return a.startDAP(avatarPath);
            });
        }

        private <T> CompletableFuture<T> relay(String id, Function<C2CClient, CompletableFuture<T>> relay) {
            return relay(id, (a, b) -> relay.apply(a));
        }

        private <T> CompletableFuture<T> relay(String id, BiFunction<C2CClient, ServerClientInterface, CompletableFuture<T>> relay) {
            Optional<ServerClientInterface> first = allClients.stream().filter(client -> client.information != null && client.information.instanceId().equals(id)).findFirst();
            if (first.isEmpty()) {
                return Util.fail(ResponseErrorCode.InvalidParams, "Unknown client by id");
            }
            ServerClientInterface u = first.get();
            return relay.apply(u.client, u);
        }

        public void setClient(ReceptionistClient client) {
            this.client = client;
        }

        @Override
        public void onDisconnect() {
            logger.info("VSCode client disconnected");
        }
    }
}
