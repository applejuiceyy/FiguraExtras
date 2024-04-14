package com.github.applejuiceyy.figuraextras.vscode.protocol;

public class WorldInformation {
    String name;
    boolean singleplayer;

    public WorldInformation(String name, boolean singleplayer) {
        this.name = name;
        this.singleplayer = singleplayer;
    }

    @Override
    public String toString() {
        return "WorldInformation{" +
                "name='" + name + '\'' +
                ", singleplayer=" + singleplayer +
                '}';
    }
}
