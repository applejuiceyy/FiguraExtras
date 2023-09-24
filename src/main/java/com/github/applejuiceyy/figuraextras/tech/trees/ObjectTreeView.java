package com.github.applejuiceyy.figuraextras.tech.trees;

import com.github.applejuiceyy.figuraextras.screen.Blocker;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.ContentPopOut;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.core.ReferenceStore;
import com.github.applejuiceyy.figuraextras.tech.trees.ui.EntryUI;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;

public abstract class ObjectTreeView<VALUE> implements InfoViews.View {
    protected final InfoViews.Context context;

    private final FlowLayout root = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));

    EntryUI<VALUE> expanderUI;

    private final DiscreteSliderComponent slider = Components.discreteSlider(Sizing.fill(70), 1, 100);
    private final CheckboxComponent checkbox = Components.checkbox(net.minecraft.network.chat.Component.literal("ticks"));

    private final ContentPopOut contentPopOut;

    public ObjectTreeView(InfoViews.Context context) {
        this.context = context;
        class SB extends ScrollContainer<FlowLayout> implements Blocker {
            protected SB(ScrollDirection direction, Sizing horizontalSizing, Sizing verticalSizing, FlowLayout child) {
                super(direction, horizontalSizing, verticalSizing, child);
            }

            @Override
            public boolean shouldBlock(double mouseX, double mouseY) {
                return this.isInScrollbar(mouseX, mouseY);
            }
        }
        contentPopOut = new ContentPopOut(context.getRoot(), context.getHost());


        expanderUI = new EntryUI<>(
                getRootExpander(),
                contentPopOut,
                new ReferenceStore(),
                getRootExpander().getRegistration()
        );

        FlowLayout container = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        container.child(expanderUI.childrenLayout);
        container.padding(Insets.right(10));
        ScrollContainer<FlowLayout> scrollable = new SB(ScrollContainer.ScrollDirection.VERTICAL, Sizing.fill(100), Sizing.fill(100), container);
        scrollable.scrollbar(ScrollContainer.Scrollbar.vanilla());
        scrollable.scrollbarThiccness(10);
        root.child(scrollable);
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

        contentPopOut.render();
    }

    @Override
    public void dispose() {
        expanderUI.dispose();
    }


    protected abstract Expander<VALUE> getRootExpander();
}
