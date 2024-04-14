package com.github.applejuiceyy.figuraextras.mixin.debugadapter.figura;

import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

@Mixin(LocalAvatarLoader.class)
public interface LocalAvatarLoaderAccessor {
    @Accessor
    static Path getLastLoadedPath() {
        throw new RuntimeException("Not injected");
    }

    ;
}
