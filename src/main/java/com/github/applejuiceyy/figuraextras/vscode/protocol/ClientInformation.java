package com.github.applejuiceyy.figuraextras.vscode.protocol;

public class ClientInformation {
    private final String id;
    String version;
    String minecraftPath;
    String figuraPath;

    boolean isConnected = false;
    WorldInformation world;

    public ClientInformation(String version, String minecraftPath, String figuraPath, String id, boolean isConnected, WorldInformation world) {
        this.version = version;
        this.minecraftPath = minecraftPath;
        this.figuraPath = figuraPath;
        this.id = id;
        this.world = world;
        this.isConnected = isConnected;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getMinecraftPath() {
        return minecraftPath;
    }

    public String getFiguraPath() {
        return figuraPath;
    }

    public WorldInformation getWorld() {
        return world;
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public String toString() {
        return "ClientInformation{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", minecraftPath='" + minecraftPath + '\'' +
                ", figuraPath='" + figuraPath + '\'' +
                ", isConnected=" + isConnected +
                ", world=" + world +
                '}';
    }
}
