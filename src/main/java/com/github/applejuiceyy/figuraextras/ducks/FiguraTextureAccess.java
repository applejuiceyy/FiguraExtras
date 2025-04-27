package com.github.applejuiceyy.figuraextras.ducks;

import net.minecraft.resources.ResourceLocation;

public interface FiguraTextureAccess {

    boolean figuraExtras$hasRealTimePendingModifications();

    boolean figuraExtras$hasPendingModifications();

    void figuraExtras$lockUpdatedTexture();

    ResourceLocation figuraExtras$getUpdatedTexture();

    void figuraExtras$refreshUpdatedTexture();

    void figuraExtras$unlockUpdatedTexture();
}
