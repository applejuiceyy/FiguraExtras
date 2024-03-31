package com.github.applejuiceyy.figuraextras.mixin.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Invoker
    void invokeRenderTooltipInternal(Font textRenderer, List<ClientTooltipComponent> tooltip, int x, int y, ClientTooltipPositioner positioner);
}
