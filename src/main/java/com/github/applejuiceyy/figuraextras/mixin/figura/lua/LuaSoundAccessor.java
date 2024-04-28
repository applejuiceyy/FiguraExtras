package com.github.applejuiceyy.figuraextras.mixin.figura.lua;


import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.resources.sounds.Sound;
import org.figuramc.figura.lua.api.sound.LuaSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LuaSound.class)
public interface LuaSoundAccessor {
    @Accessor
    SoundBuffer getBuffer();

    @Accessor
    Sound getSound();
}
