package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.InstructionChartComponent;
import com.github.applejuiceyy.figuraextras.ducks.InstructionsAccess;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import org.figuramc.figura.avatar.Avatar;

public class MetricsView implements InfoViews.View {
    private final InfoViews.Context context;
    private final Avatar.Instructions instructions;
    private final FlowLayout root = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
    private final InstructionChartComponent chart;
    private final Runnable unsub;

    public MetricsView(InfoViews.Context context, Avatar.Instructions instructions) {
        this.context = context;
        this.instructions = instructions;
        chart = (InstructionChartComponent) new InstructionChartComponent()
                .sizing(Sizing.fill(100), Sizing.fill(100));
        root.child(chart);
        this.unsub = ((InstructionsAccess) instructions).addHook(chart::consumeEntry);
    }

    @Override
    public void tick() {

    }

    @Override
    public Component getRoot() {
        return root;
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
