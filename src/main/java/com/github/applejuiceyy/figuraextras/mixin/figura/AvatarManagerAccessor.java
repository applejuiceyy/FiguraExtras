package com.github.applejuiceyy.figuraextras.mixin.figura;

import org.figuramc.figura.avatar.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

@Mixin(AvatarManager.class)
public interface AvatarManagerAccessor {
    @Accessor("FETCHED_USERS")
    static Set<UUID> getFetchedUsers() {
        throw new IllegalStateException("Not injected");
    }

    ;
}
