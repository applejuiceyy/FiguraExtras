package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.InstructionChartComponent;
import com.github.applejuiceyy.figuraextras.ducks.InstructionsAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import org.figuramc.figura.avatar.Avatar;

public class MetricsView implements Lifecycle {
    private final InfoViews.Context context;
    private final Avatar.Instructions instructions;

    private final InstructionChartComponent chart;
    private final Runnable unsub;

    public MetricsView(InfoViews.Context context, ParentElement.AdditionPoint additionPoint, Avatar.Instructions instructions) {
        this.context = context;
        this.instructions = instructions;
        chart = new InstructionChartComponent();
        this.unsub = ((InstructionsAccess) instructions).figuraExtrass$addHook(chart::consumeEntry);

        additionPoint.accept(chart);
    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {

    }

    @Override
    public void dispose() {
        unsub.run();
        chart.dispose();
    }
}
