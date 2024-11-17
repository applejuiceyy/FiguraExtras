package com.github.applejuiceyy.figuraextras.components.graph;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
public abstract class DataCollection extends InGraphRendererBaker {
    abstract @Nullable Bounds getDataBounds(boolean hintWantsMinX, boolean hintWantsMinY, boolean hintWantsMaxX, boolean hintWantsMaxY);
}