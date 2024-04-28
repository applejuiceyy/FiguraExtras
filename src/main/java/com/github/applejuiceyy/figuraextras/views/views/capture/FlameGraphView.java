package com.github.applejuiceyy.figuraextras.views.views.capture;

import com.github.applejuiceyy.figuraextras.components.FlameGraphComponent;
import com.github.applejuiceyy.figuraextras.components.RangeSliderComponent;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.GraphBuilder;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;


public class FlameGraphView implements Lifecycle {

    private final Grid root;
    FlameGraphComponent component;
    RangeSliderComponent slider;

    public FlameGraphView(InfoViews.Context context, ParentElement.AdditionPoint additionPoint, GraphBuilder.Frame frame) {
        root = new Grid();

        root.rows()
                .percentage(1)
                .content()
                .cols()
                .percentage(1);

        additionPoint.accept(root);

        component = new FlameGraphComponent(frame) {
        };

        root.add(component);

        slider = new RangeSliderComponent();
        slider.setMax(frame.getInstructions());
        slider.setMinSpacing(10);

        Elements.TwoWaySetter.track(slider.lowerKnob, component.viewStart);
        Elements.TwoWaySetter.track(slider.higherKnob, component.viewEnd);

        root.add(slider).setRow(1);
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
