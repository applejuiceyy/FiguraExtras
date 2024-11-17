package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import net.minecraft.client.gui.GuiGraphics;

public interface InGraphRenderer {
    void render(GuiGraphics context);
    boolean doTooltip(DefaultCancellableEvent.ToolTipEvent toolTipEvent, double x, double y);
    boolean handleMouseDown(double x, double y, int button);
}
