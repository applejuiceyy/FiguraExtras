package com.github.applejuiceyy.figuraextras.ducks;

import java.nio.ByteBuffer;

public interface SoundBufferAccess {
    void figuraExtras$keepBuffer();

    ByteBuffer figuraExtras$getKeptBuffer();
}
