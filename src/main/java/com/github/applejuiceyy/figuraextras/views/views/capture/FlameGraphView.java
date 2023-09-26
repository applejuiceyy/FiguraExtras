package com.github.applejuiceyy.figuraextras.views.views.capture;

import com.github.applejuiceyy.figuraextras.components.FlameGraphComponent;
import com.github.applejuiceyy.figuraextras.components.RangeSliderComponent;
import com.github.applejuiceyy.figuraextras.tech.captures.captures.FlameGraph;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;


public class FlameGraphView implements InfoViews.View {

    private final FlowLayout root;
    FlameGraphComponent component;
    RangeSliderComponent slider;

    public FlameGraphView(InfoViews.Context context, FlameGraph.Frame frame) {
        component = new FlameGraphComponent(frame) {
            @Override
            protected void frameSelected(FlameGraph.Frame a, Integer b) {
                slider.lowerKnob = b - 10;
                slider.higherKnob = b + a.getInstructions() + 10;
            }
        };
        component.sizing(Sizing.fill(100), Sizing.fill(90));

        root = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        root.child(component);

        slider = new RangeSliderComponent();
        slider.setMax(frame.getInstructions());
        slider.setMinSpacing(10);
        slider.sizing(Sizing.fill(100), Sizing.fill(10));
        root.child(slider);
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
        component.viewStart = slider.lowerKnob;
        component.viewEnd = slider.higherKnob;
    }

    @Override
    public void dispose() {

    }
}
