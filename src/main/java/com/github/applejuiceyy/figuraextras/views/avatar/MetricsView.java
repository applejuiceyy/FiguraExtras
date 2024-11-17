package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.components.graph.Axis;
import com.github.applejuiceyy.figuraextras.components.graph.GraphComponent;
import com.github.applejuiceyy.figuraextras.components.graph.LineCollection;
import com.github.applejuiceyy.figuraextras.ducks.InstructionsAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;

import java.util.Objects;

public class MetricsView implements Lifecycle {
    private final GraphComponent chart;
    private final Runnable unsub;
    private final long start;
    private boolean collecting = true;

    public MetricsView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint, Avatar.Instructions instructions) {
        Grid root = new Grid();

        root.rows().content().percentage(1).cols().percentage(1);

        Button button = Button.minimal();
        button.addAnd("Pause");
        button.activation.subscribe(eitherCausedEvent -> {
            collecting = !collecting;
            button.setText(Component.literal(collecting ? "Pause" : "Resume"));
        });

        root.add(button);

        chart = new GraphComponent();
        root.add(chart).setRow(1);


        LineCollection lineCollection = LineCollection.of(0xff776622, 0xffff6622);
        chart.addCollection(lineCollection);
        ((Axis.DefaultAxis) Objects.requireNonNull(chart.getAxisRenderer())).setBottom(Axis.SideKind.NOTCHES);

        this.start = System.currentTimeMillis();

        this.unsub = ((InstructionsAccess) instructions).figuraExtrass$addHook(i -> {
            if(!collecting) return;
            long l = System.currentTimeMillis();
            float o = l - start;
            lineCollection.add(new LineCollection.DataPoint() {
                @Override
                public float getY() {
                    return i;
                }

                @Override
                public float getX() {
                    return o;
                }

                @Override
                public void onHover(DefaultCancellableEvent.ToolTipEvent event) {
                    event.add(Component.literal(i + " instructions"));
                }
            });
            while(lineCollection.size() > 0) {
                LineCollection.DataPoint dataPoint = lineCollection.get(0);
                if(dataPoint.getX() < (l - start) - 5000) {
                    lineCollection.remove(0);
                }
                else {
                    break;
                }
            }

        });

        additionPoint.accept(root);
    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {
        if(!collecting) return;
        long l = System.currentTimeMillis();
        chart.setXMin((float) (l - start - 5000));
        chart.setXMax((float) (l - start));
    }

    @Override
    public void dispose() {
        unsub.run();
    }
}
