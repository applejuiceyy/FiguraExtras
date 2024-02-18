package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import org.figuramc.figura.lua.api.net.HttpRequestsAPI;
import org.figuramc.figura.lua.api.net.NetworkingAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = HttpRequestsAPI.class, remap = false)
public interface HttpRequestsAPIAccessor {
    @Accessor
    NetworkingAPI getParent();
}
