package com.github.applejuiceyy.figuraextras.ipc.protocol;

import com.github.applejuiceyy.figuraextras.ipc.DisconnectAware;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public interface C2CServer extends DisconnectAware, Endpoint {
    @JsonNotification
    void disallowSuccession(boolean disallow);

    @JsonNotification
    void updateInfo(ClientInformation information);

    @JsonDelegate
    C2CServerBackend getBackend();

    @JsonSegment("backend")
    interface C2CServerBackend extends Endpoint {
        @JsonRequest
        CompletableFuture<Void> ping(byte[] content);

        @JsonRequest
        CompletableFuture<Void> subscribe(String id);

        @JsonRequest
        CompletableFuture<Void> unsubscribe(String id);

        // transmuted from https
        @JsonRequest
        CompletableFuture<String> getApi();

        @JsonRequest
        CompletableFuture<String> getMotd();

        @JsonRequest
        CompletableFuture<String> getVersion();

        @JsonRequest
        CompletableFuture<String> getLimits();

        @JsonRequest
        CompletableFuture<String> deleteAvatar(HashMap<String, String> args);

        @JsonRequest
        CompletableFuture<String> putAvatar(HashMap<String, String> args);

        @JsonRequest
        CompletableFuture<String> setEquip(HashMap<String, String> args);

        @JsonRequest
        CompletableFuture<String> getUser(HashMap<String, String> args);

        @JsonRequest
        CompletableFuture<String> getAvatar(HashMap<String, String> args);
    }
}
