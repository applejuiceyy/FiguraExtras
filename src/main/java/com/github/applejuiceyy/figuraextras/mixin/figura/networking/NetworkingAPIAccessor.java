package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.net.NetworkingAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = NetworkingAPI.class, remap = false)
public interface NetworkingAPIAccessor {
    @Accessor
    Avatar getOwner();
}
