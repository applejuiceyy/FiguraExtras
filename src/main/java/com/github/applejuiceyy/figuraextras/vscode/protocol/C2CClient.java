package com.github.applejuiceyy.figuraextras.vscode.protocol;

import com.github.applejuiceyy.figuraextras.vscode.DisconnectAware;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

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
}
