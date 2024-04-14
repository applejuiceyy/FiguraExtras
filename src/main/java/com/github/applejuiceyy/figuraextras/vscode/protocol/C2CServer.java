package com.github.applejuiceyy.figuraextras.vscode.protocol;

import com.github.applejuiceyy.figuraextras.vscode.DisconnectAware;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

public interface C2CServer extends DisconnectAware {
    @JsonNotification
    void disallowSuccession(boolean disallow);

    @JsonNotification
    void updateInfo(ClientInformation information);
}
