package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.components.InstructionChartComponent;
import com.github.applejuiceyy.figuraextras.ducks.InstructionsAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import org.figuramc.figura.avatar.Avatar;

public class MetricsView implements Lifecycle {
    private final InstructionChartComponent chart;
    private final Runnable unsub;

    public MetricsView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint, Avatar.Instructions instructions) {
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
