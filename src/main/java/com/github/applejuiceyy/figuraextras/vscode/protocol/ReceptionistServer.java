package com.github.applejuiceyy.figuraextras.vscode.protocol;

import com.github.applejuiceyy.figuraextras.vscode.DisconnectAware;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ReceptionistServer extends DisconnectAware {
    @JsonRequest
    CompletableFuture<List<ClientInformation>> getClients();

    @JsonRequest
    CompletableFuture<List<WorldInformation>> getClientWorlds(String id);

    @JsonRequest
    CompletableFuture<Void> joinSinglePlayerWorld(String id, String name);

    @JsonRequest
    CompletableFuture<String> startDAP(String id, String avatarPath);
}
