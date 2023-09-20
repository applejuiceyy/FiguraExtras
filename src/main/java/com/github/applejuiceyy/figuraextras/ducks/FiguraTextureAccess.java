package com.github.applejuiceyy.figuraextras.ducks;

import net.minecraft.resources.ResourceLocation;

public interface FiguraTextureAccess {
    boolean figuraExtrass$hasPendingModifications();

    void figuraExtrass$lockUpdatedTexture();

    ResourceLocation figuraExtrass$getUpdatedTexture();

    void figuraExtrass$refreshUpdatedTexture();

    void figuraExtrass$unlockUpdatedTexture();
}
