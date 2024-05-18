package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import org.figuramc.figura.backend2.NetworkStuff;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NetworkStuff.class)
public interface NetworkStuffAccessor {
    @Invoker
    static void invokeAuthSuccess(String token) {
        throw new IllegalStateException();
    }

    ;
}
