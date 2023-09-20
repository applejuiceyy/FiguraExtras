package com.github.applejuiceyy.figuraextras.mixin.gui.owolib;

import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.util.CursorAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OwoUIAdapter.class)
public interface OwoUIAdapterAccessor {
    @Accessor
    CursorAdapter getCursorAdapter();
}
