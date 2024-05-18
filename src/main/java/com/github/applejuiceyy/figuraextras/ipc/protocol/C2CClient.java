package com.github.applejuiceyy.figuraextras.ipc.protocol;

import com.github.applejuiceyy.figuraextras.ipc.DisconnectAware;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface C2CClient extends DisconnectAware {
    @JsonNotification
    void successionIndex(int index);

    @JsonRequest
    CompletableFuture<List<WorldInformation>> getClientWorlds();

    @JsonRequest
    CompletableFuture<Void> joinSinglePlayerWorld(String name);

    @JsonRequest
    CompletableFuture<String> startDAP(String avatarPath);

    @JsonDelegate
    C2CClientBackend getBackend();

    @JsonSegment("backend")
    interface C2CClientBackend extends Endpoint {
        @JsonRequest
        CompletableFuture<Void> websocketEvent(byte[] bytes);
    }
}
