package com.github.applejuiceyy.figuraextras.ducks;

import com.mojang.blaze3d.platform.WindowEventHandler;
import org.jetbrains.annotations.Nullable;

public interface VirtualScreenAccess {
    void figuraExtras$setEventListener(@Nullable WindowEventHandler handler);
}
