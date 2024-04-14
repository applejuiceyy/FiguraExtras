package com.github.applejuiceyy.figuraextras.mixin.figura.typemanager;

import com.github.applejuiceyy.figuraextras.ducks.LuaTypeManagerAccess;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaTypeManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LuaTypeManager.class)
public class LuaTypeManagerMixin implements LuaTypeManagerAccess {
    @Unique
    @Nullable Avatar avatar;

    @Override
    public void figuraExtrass$setAvatar(@Nullable Avatar avatar) {
        this.avatar = avatar;
    }

    @Override
    public @Nullable Avatar figuraExtrass$getAvatar() {
        return avatar;
    }
}
