package com.github.applejuiceyy.figuraextras.vscode;

import com.github.applejuiceyy.figuraextras.util.Util;
import com.github.applejuiceyy.figuraextras.vscode.ipc.IPCFactory;
import com.github.applejuiceyy.figuraextras.vscode.protocol.*;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Tuple;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class ReceptionistServer {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:ReceptionistServer");
    private static C2CClientImpl currentClient;
    ArrayList<ServerClientInterface> eligibleToSuccession = new ArrayList<>();
    ArrayList<ServerClientInterface> allClients = new ArrayList<>();
    ArrayList<ReceptionistVSCInterface> vscInterfaces = new ArrayList<>();

    public ReceptionistServer() {

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
            IPCFactory.IPC otherClientServer = ipcFactory.createServer(OTHER2HOST_PATH);
            IPCFactory.IPC receptionistPipeServer = ipcFactory.createServer(RECEPTIONIST_PATH);
            otherClientServer.continuousConnect(receptionistServer::otherClientConnection);
            receptionistPipeServer.continuousConnect(receptionistServer::vscodeConnection);
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
                throw new RuntimeException(e);
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
        startWithTermination(local, i, o, launcher);
        return launcher.getRemoteProxy();
    }

    public static <T> void startWithTermination(Object local, InputStream i, OutputStream o, Launcher<T> launcher) {
        listenForCompletion(launcher.startListening()).thenRun(() -> {
            if (local instanceof DisconnectAware da) {
                da.onDisconnect();
            }
            try {
                i.close();
            } catch (IOException ignored) {
            }
            try {
                o.close();
            } catch (IOException ignored) {
            }
        });
    }

    void otherClientConnection(InputStream i, OutputStream o) {
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

    class ServerClientInterface implements C2CServer {
        private C2CClient client;
        private boolean successionDisallowed = false;

        private @Nullable ClientInformation information;

        public ServerClientInterface(InputStream i, OutputStream o) {
        }

        @Override
        public void disallowSuccession(boolean disallow) {
            logger.info("Minecraft client wished to " + (disallow ? "disallow" : "allow") + " succession");
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
            logger.info("Minecraft client wished to update identity: " + information);
            this.information = information;
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
    }

    class ReceptionistVSCInterface implements com.github.applejuiceyy.figuraextras.vscode.protocol.ReceptionistServer {

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
            Optional<ServerClientInterface> first = allClients.stream().filter(client -> client.information != null && client.information.getId().equals(id)).findFirst();
            if (first.isEmpty()) {
                return Util.fail(ResponseErrorCode.InvalidParams, "Unknown client by id");
            }
            return first.get().client.getClientWorlds();
        }

        @Override
        public CompletableFuture<Void> joinSinglePlayerWorld(String id, String name) {
            Optional<ServerClientInterface> first = allClients.stream().filter(client -> client.information != null && client.information.getId().equals(id)).findFirst();
            if (first.isEmpty()) {
                return Util.fail(ResponseErrorCode.InvalidParams, "Unknown client by id");
            }
            return first.get().client.joinSinglePlayerWorld(name);
        }

        @Override
        public CompletableFuture<String> startDAP(String id, String avatarPath) {
            Optional<ServerClientInterface> first = allClients.stream().filter(client -> client.information != null && client.information.getId().equals(id)).findFirst();
            if (first.isEmpty()) {
                return Util.fail(ResponseErrorCode.InvalidParams, "Unknown client by id");
            }

            ServerClientInterface serverClientInterface = first.get();
            assert serverClientInterface.information != null;
            if (serverClientInterface.information.getWorld() == null || serverClientInterface.information.isConnected()) {
                return Util.fail(ResponseErrorCode.InvalidParams, "Invalid client");
            }

            return serverClientInterface.client.startDAP(avatarPath);
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
