package com.github.applejuiceyy.figuraextras.mixin.figura;

import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.avatar.AbstractAvatarWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractAvatarWidget.class, remap = false)
public interface AvatarWidgetAccessor {
    @Accessor("context")
    ContextMenu getContextMenu();
}
