package com.github.applejuiceyy.figuraextras.mixin;

import com.github.applejuiceyy.figuraextras.ducks.SoundEngineAccess;
import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = SoundEngine.class, priority = 1001)
public class SoundEngineMixin implements SoundEngineAccess {
    @Shadow
    @Final
    private ChannelAccess channelAccess;

    @Shadow
    @Dynamic
    @Final
    private List<LuaSound> figuraHandlers;

    @Override
    public ChannelAccess.ChannelHandle figuraExtrass$createHandle(Library.Pool mode) {
        return channelAccess.createHandle(mode).join();
    }

    @Override
    public List<LuaSound> figuraExtrass$getFiguraHandles() {
        return figuraHandlers;
    }
}
