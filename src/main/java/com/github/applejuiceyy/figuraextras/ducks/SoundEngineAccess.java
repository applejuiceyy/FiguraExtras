package com.github.applejuiceyy.figuraextras.ducks;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.sounds.ChannelAccess;
import org.figuramc.figura.lua.api.sound.LuaSound;

import java.util.List;

public interface SoundEngineAccess {
    ChannelAccess.ChannelHandle figuraExtrass$createHandle(Library.Pool mode);

    List<LuaSound> figuraExtrass$getFiguraHandles();
}
