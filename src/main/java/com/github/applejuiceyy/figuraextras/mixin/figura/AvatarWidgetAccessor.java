package com.github.applejuiceyy.figuraextras.mixin.figura;

import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.avatar.AbstractAvatarWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractAvatarWidget.class)
public interface AvatarWidgetAccessor {
    @Accessor("context")
    ContextMenu getContextMenu();
}
