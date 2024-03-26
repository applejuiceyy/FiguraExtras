package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.InstructionChartComponent;
import com.github.applejuiceyy.figuraextras.ducks.InstructionsAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import org.figuramc.figura.avatar.Avatar;

public class MetricsView implements InfoViews.View {
    private final InfoViews.Context context;
    private final Avatar.Instructions instructions;
    private final Flow root = new Flow();
    private final InstructionChartComponent chart;
    private final Runnable unsub;

    public MetricsView(InfoViews.Context context, Avatar.Instructions instructions) {
        this.context = context;
        this.instructions = instructions;
        chart = new InstructionChartComponent();
        root.add(chart);
        this.unsub = ((InstructionsAccess) instructions).figuraExtrass$addHook(chart::consumeEntry);
    }

    @Override
    public void tick() {

    }

    @Override
    public Element getRoot() {
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
