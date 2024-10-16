package com.github.applejuiceyy.figuraextras.mixin;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.ducks.SoundBufferAccess;
import com.mojang.blaze3d.audio.SoundBuffer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;

@Mixin(SoundBuffer.class)
public class SoundBufferMixin implements SoundBufferAccess {
    @Unique
    ByteBuffer keptBuffer = null;
    @Shadow
    private @Nullable ByteBuffer data;

    @Override
    public void figuraExtrass$keepBuffer() {
        if (data == null) {
            FiguraExtras.logger.warn("KeepBuffer called too late, data already gone");
            return;
        }
        keptBuffer = data;
    }

    @Override
    public ByteBuffer figuraExtrass$getKeptBuffer() {
        return keptBuffer;
    }
}
