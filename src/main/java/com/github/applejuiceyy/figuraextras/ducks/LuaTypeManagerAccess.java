package com.github.applejuiceyy.figuraextras.ducks;

import org.figuramc.figura.avatar.Avatar;
import org.jetbrains.annotations.Nullable;

public interface LuaTypeManagerAccess {
    void figuraExtrass$setAvatar(Avatar avatar);

    @Nullable Avatar figuraExtrass$getAvatar();
}
