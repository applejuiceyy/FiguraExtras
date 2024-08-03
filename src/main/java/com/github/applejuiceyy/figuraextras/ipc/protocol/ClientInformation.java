package com.github.applejuiceyy.figuraextras.ipc.protocol;

public record ClientInformation(String version, String minecraftPath, String figuraPath, String instanceId,
                                String offlinePlayerId, String gameProfileId, boolean isConnected,
                                WorldInformation world) {
}
