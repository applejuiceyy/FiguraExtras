package com.github.applejuiceyy.figuraextras.components.graph;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
public abstract class DataCollection extends Invalidates {
    abstract @Nullable Bounds getDataBounds(boolean hintWantsMinX, boolean hintWantsMinY, boolean hintWantsMaxX, boolean hintWantsMaxY);
    abstract BakedDataRendering getBaked(BakingContext context);

    public interface BakedDataRendering {
        void render(GuiGraphics context);
        boolean doTooltip(DefaultCancellableEvent.ToolTipEvent toolTipEvent, double x, double y);
        boolean handleMouseDown(double x, double y, int button);
    }
}
