package com.github.applejuiceyy.figuraextras.ipc;

import com.github.applejuiceyy.figuraextras.mixin.figura.AvatarManagerAccessor;
import org.figuramc.figura.backend2.NetworkStuff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DivertedBackendService {
    public static Logger logger = LoggerFactory.getLogger("FiguraExtras:IPCManager-DBS");

    private final IPCManager ipcManager;
    private boolean divertBackend = false;
    private boolean backendConnected = false;

    public DivertedBackendService(IPCManager ipcManager) {
        this.ipcManager = ipcManager;
    }

    public boolean shouldDivertBackend() {
        return divertBackend;
    }

    public boolean isBackendConnected() {
        return backendConnected;
    }

    public void divertBackend(boolean divert) {
        if (divert == this.divertBackend) {
            return;
        }
        if (ipcManager.isConnected()) {
            NetworkStuff.disconnect("Diverting");
            this.divertBackend = divert;
            AvatarManagerAccessor.getFetchedUsers().clear();
            NetworkStuff.auth();
        } else {
            this.divertBackend = divert;
        }
    }

    public void connectDivertedBackend() {
        logger.info("Connecting to diverted backend");
        ipcManager.getC2CServer().getBackend().connect().handle((e, c) -> {
            backendConnected = true;
            return null;
        });
    }

    public void disconnectDivertedBackend() {
        logger.info("Disconnecting from diverted backend");
        ipcManager.getC2CServer().getBackend().disconnect().handle((e, c) -> {
            backendConnected = false;
            return null;
        });
    }

    void closeEverything() {
        disconnect(false);
    }

    void syncWithNewServer() {
        if (divertBackend) {
            backendConnected = false;  // this is a new server so already disconnected
            NetworkStuff.disconnect("Reconnecting");
            AvatarManagerAccessor.getFetchedUsers().clear();
            NetworkStuff.auth();
        }
    }

    void disconnect(boolean crashedOut) {
        if (crashedOut) {
            backendConnected = false;
            return;
        }
        if (backendConnected) {
            disconnectDivertedBackend();
        }
    }
}
