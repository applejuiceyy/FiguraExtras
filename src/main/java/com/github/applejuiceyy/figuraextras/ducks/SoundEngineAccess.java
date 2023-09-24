package com.github.applejuiceyy.figuraextras.ducks;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.sounds.ChannelAccess;

public interface SoundEngineAccess {
    ChannelAccess.ChannelHandle figuraExtrass$createHandle(Library.Pool mode);
}
