package com.github.applejuiceyy.figuraextras.mixin;

import com.github.applejuiceyy.figuraextras.ducks.SoundEngineAccess;
import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundEngine.class)
public class SoundEngineMixin implements SoundEngineAccess {
    @Shadow
    @Final
    private ChannelAccess channelAccess;

    @Override
    public ChannelAccess.ChannelHandle figuraExtrass$createHandle(Library.Pool mode) {
        return channelAccess.createHandle(mode).join();
    }
}
