package com.github.applejuiceyy.figuraextras.views.views.capture;

import com.github.applejuiceyy.figuraextras.components.FlameGraphComponent;
import com.github.applejuiceyy.figuraextras.components.RangeSliderComponent;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.FlameGraph;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;


public class FlameGraphView implements Lifecycle {

    private final Grid root;
    FlameGraphComponent component;
    RangeSliderComponent slider;

    public FlameGraphView(InfoViews.Context context, ParentElement.AdditionPoint additionPoint, FlameGraph.Frame frame) {
        root = new Grid();

        root.rows()
                .percentage(1)
                .content()
                .cols()
                .content();

        additionPoint.accept(root);

        // TODO

        /*component = new FlameGraphComponent(frame) {
            @Override
            protected void frameSelected(FlameGraph.Frame a, Integer b) {
                slider.lowerKnob = b - 10;
                slider.higherKnob = b + a.getInstructions() + 10;
            }
        };
        component.sizing(Sizing.fill(100), Sizing.fill(90));

        root = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        root.add(component);

        slider = new RangeSliderComponent();
        slider.setMax(frame.getInstructions());
        slider.setMinSpacing(10);
        slider.sizing(Sizing.fill(100), Sizing.fill(10));
        root.child(slider);*/
    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {
        component.viewStart = slider.lowerKnob;
        component.viewEnd = slider.higherKnob;
    }

    @Override
    public void dispose() {

    }
}
