package com.github.applejuiceyy.figuraextras.ipc;

import com.github.applejuiceyy.figuraextras.ipc.backend.ReceptionistServerBackend;
import com.github.applejuiceyy.figuraextras.ipc.protocol.*;
import com.github.applejuiceyy.figuraextras.util.Util;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.applejuiceyy.figuraextras.ipc.JSONPUtils.configureLauncher;

public class ReceptionistServer implements AutoCloseable {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:ReceptionistServer");

    ArrayList<ServerClientInterface> allClients = new ArrayList<>();
    ArrayList<ReceptionistVSCInterface> vscInterfaces = new ArrayList<>();
    ReceptionistServerBackend backend = new ReceptionistServerBackend();

    public ReceptionistServer() {

    }


    public void otherClientConnection(InputStream i, OutputStream o) {
        logger.info("Other minecraft client connected");
        ServerClientInterface e = new ServerClientInterface(() -> Util.closeMultiple(i, o));
        allClients.add(e);
        configureLauncher(e, C2CClient.class, i, o, e::setClient);
    }

    public void vscodeConnection(InputStream i, OutputStream o) {
        logger.info("VSCode instance connected");
        ReceptionistVSCInterface e = new ReceptionistVSCInterface(i, o);
        vscInterfaces.add(e);
        configureLauncher(e, ReceptionistClient.class, i, o, e::setClient);
    }

    public C2CServer selfConnect(C2CClientImpl C2CClient) {
        logger.info("Self-connected");
        ServerClientInterface e = new ServerClientInterface(C2CClient::onDisconnect);
        allClients.add(e);
        C2CClient.setServer(e);
        return e;
    }

    public ReceptionistServerBackend getBackend() {
        return backend;
    }

    @Override
    public void close() throws Exception {
        AutoCloseable[] autos = new AutoCloseable[vscInterfaces.size() * 2 + allClients.size()];
        int i = 0;
        for (ServerClientInterface a : allClients) {
            autos[i] = a.closer;
            i += 2;
        }
        for (ReceptionistVSCInterface a : vscInterfaces) {
            autos[i] = a.input;
            autos[i + 1] = a.output;
            i += 2;
        }
        Util.closeMultiple(autos);
    }



    public class ServerClientInterface implements C2CServer {
        final ReceptionistServerBackendConnection receptionistServerBackendConnection = new ReceptionistServerBackendConnection(this, ReceptionistServer.this);
        private final AutoCloseable closer;
        C2CClient client;
        @Nullable
        ClientInformation information;

        public ServerClientInterface(AutoCloseable closer) {
            this.closer = closer;
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
        }

        @Override
        public void onDisconnect() {
            logger.info("Minecraft client disconnected");
            allClients.remove(this);
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
        private final InputStream input;
        private final OutputStream output;

        public ReceptionistVSCInterface(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
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
