package com.github.applejuiceyy.figuraextras.ipc;

import com.github.applejuiceyy.figuraextras.ipc.protocol.C2CServer;
import com.github.applejuiceyy.figuraextras.ipc.protocol.ClientInformation;
import com.github.applejuiceyy.figuraextras.ipc.underlying.IPCFactory;
import com.github.applejuiceyy.figuraextras.util.Util;
import net.minecraft.util.Tuple;
import org.figuramc.figura.gui.FiguraToast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class IPCManager {
    public static final IPCManager INSTANCE = new IPCManager();
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:IPCManager");
    public final Set<AutoCloseable> closeables = new HashSet<>();
    public final DivertedBackendService divertBackend = new DivertedBackendService(this);
    private final Random random = new Random();
    private boolean enabled = false;
    private boolean connectionTransition = false;
    private boolean isHost = false;
    private boolean connected = false;
    private int tries = 0;
    private IPCFactory.IPC clientClientServer = null;
    private IPCFactory.IPC clientVscodeServer = null;
    private ReceptionistServer receptionistServer = null;
    private C2CServer C2CServer = null;
    private C2CClientImpl C2CClient = null;
    private Tuple<InputStream, OutputStream> clientIOBundle;
    private ClientInformation currentInformation;

    private IPCManager() {

    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConnectionTransitioning() {
        return connectionTransition;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isHost() {
        return isHost;
    }

    /***
     * Only returns non-null if isHost is true
     * @return the receptionist server
     */
    public ReceptionistServer getReceptionistServer() {
        return receptionistServer;
    }

    /***
     * Only returns non-null if it's connected
     * @return the receptionist server
     */
    public com.github.applejuiceyy.figuraextras.ipc.protocol.C2CServer getC2CServer() {
        return C2CServer;
    }

    /***
     * Only returns non-null if it's connected
     * @return the receptionist server
     */
    public C2CClientImpl getC2CClient() {
        return C2CClient;
    }

    public void setCurrentInformation(ClientInformation information) {
        this.currentInformation = information;
        if (connected) {
            C2CServer.updateInfo(currentInformation);
        }
    }

    public void enableConnections(boolean enable) {
        if (enabled == enable) {
            return;
        }
        enabled = enable;
        tries = 0;
        assertStatesTrampoline();
    }


    public void closeEverything() {
        divertBackend.closeEverything();
        enableConnections(false);
        Util.closeMultiple(closeables.toArray(new AutoCloseable[0]));
        closeables.clear();
    }

    private void syncWithNewServer() {
        C2CServer.updateInfo(currentInformation);
        divertBackend.syncWithNewServer();
    }

    private void assertStatesTrampoline() {
        while (assertStates()) ;
    }

    private boolean assertStates() {
        if (connectionTransition) return false;

        if (!enabled && connected) {
            disconnect(false);
        } else if (enabled && !connected) {
            try {
                attemptConnection();
            } catch (IPCFactory.UnsupportedPlatformException e) {
                FiguraToast.sendToast("Cannot connect to other clients as platform is not supported");
                enabled = false;
                return false;
            } catch (Exception exception) {
                tries++;
                if (tries >= 5) {
                    FiguraToast.sendToast("Cannot connect to other clients, view the logs for more information");
                    logger.error("Could not connect to other clients", exception);
                    enabled = false;
                    return false;
                } else {
                    connectionTransition = true;
                    logger.warn("Failed at connecting, trying again in 5 seconds");
                    Util.after(() -> {
                        connectionTransition = false;
                        assertStatesTrampoline();
                    }, 5000);
                }
            }
            if (!connectionTransition) {
                syncWithNewServer();
            }
        } else {
            return false;
        }

        return true;
    }

    private void attemptConnection() throws IPCFactory.UnsupportedPlatformException {
        logger.info("Attempting a connection");
        connectionTransition = true;
        if (_attemptConnection()) {
            isHost = true;
        }
        ;
        connectionTransition = false;
        connected = true;
    }

    private boolean _attemptConnection() throws IPCFactory.UnsupportedPlatformException {
        String RECEPTIONIST_PATH = "figuraextras\\receptionist";
        String OTHER2HOST_PATH = "figuraextras\\C2CServer";

        IPCFactory ipcFactory = IPCFactory.getIPCFactory();

        boolean isHost = false;

        if (!ipcFactory.exists(OTHER2HOST_PATH)) {
            isHost = true;
            ReceptionistServer receptionistServer = new ReceptionistServer();

            try {
                clientClientServer = ipcFactory.createServer(OTHER2HOST_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                clientVscodeServer = ipcFactory.createServer(RECEPTIONIST_PATH);
            } catch (IOException e) {
                try {
                    clientClientServer.close();
                } catch (IOException ignored) {
                }
                clientClientServer = null;
                throw new RuntimeException(e);
            }

            clientClientServer.continuousConnect(receptionistServer::otherClientConnection);
            clientVscodeServer.continuousConnect(receptionistServer::vscodeConnection);

            this.receptionistServer = receptionistServer;

            C2CClient = new C2CClientImpl(() -> {
                if (!connectionTransition && connected) {
                    disconnect(true);
                    connectionTransition = true;
                    Util.after(() -> {
                        connectionTransition = false;
                        assertStatesTrampoline();
                    }, random.nextInt(1000, 2000));

                }
            });
            C2CServer = receptionistServer.selfConnect(C2CClient);
        } else {
            try {
                clientIOBundle = ipcFactory.connectAsClient(OTHER2HOST_PATH);
                C2CClient = new C2CClientImpl(() -> {
                    if (!connectionTransition && connected) {
                        disconnect(true);
                        connectionTransition = true;
                        Util.after(() -> {
                            connectionTransition = false;
                            assertStatesTrampoline();
                        }, random.nextInt(1000, 2000));

                    }
                });
                C2CServer = JSONPUtils.configureLauncher(
                        C2CClient,
                        C2CServer.class,
                        clientIOBundle.getA(),
                        clientIOBundle.getB(),
                        C2CClient::setServer
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        return isHost;
    }

    private void disconnect(boolean crashedOut) {
        logger.info("Disconnecting");
        if (!connected) {
            return;
        }

        if (crashedOut) {
            C2CClient = null;
            C2CServer = null;
        }

        divertBackend.disconnect(crashedOut);

        connectionTransition = true;
        connected = false;
        isHost = false;


        if (receptionistServer != null) {
            try {
                Util.closeMultiple(receptionistServer, clientVscodeServer, clientClientServer);
            } catch (RuntimeException e) {
                logger.error("Ignoring host closing errors", e);
            }
            clientVscodeServer = null;
            clientClientServer = null;
            receptionistServer = null;
        } else {
            try {
                Util.closeMultiple(clientIOBundle.getA(), clientIOBundle.getB());
            } catch (RuntimeException e) {
                logger.error("Ignoring networking closing error", e);
            }
            clientIOBundle = null;
        }

        C2CClient = null;
        C2CServer = null;

        connectionTransition = false;
    }
}
