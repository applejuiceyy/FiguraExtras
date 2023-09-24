package com.github.applejuiceyy.figuraextras.mixin;

import com.mojang.blaze3d.audio.SoundBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.sound.sampled.AudioFormat;
import java.util.OptionalInt;

@Mixin(SoundBuffer.class)
public interface SoundBufferAccessor {
    @Accessor("format")
    AudioFormat getAudioFormat();

    @Invoker
    OptionalInt invokeGetAlBuffer();
}
