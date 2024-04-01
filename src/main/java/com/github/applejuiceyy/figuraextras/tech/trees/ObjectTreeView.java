package com.github.applejuiceyy.figuraextras.tech.trees;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.ContentPopOut;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.tech.trees.core.Expander;
import com.github.applejuiceyy.figuraextras.tech.trees.core.ReferenceStore;
import com.github.applejuiceyy.figuraextras.tech.trees.ui.EntryUI;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;


public abstract class ObjectTreeView<VALUE> implements Lifecycle {
    protected final InfoViews.Context context;

    private final Grid root = new Grid();

    EntryUI<VALUE> expanderUI;

    private final ContentPopOut contentPopOut;

    public ObjectTreeView(InfoViews.Context context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;

        root.rows()
                .content()
                .cols()
                .percentage(1)
                .content();

        // TODO: implement this
        contentPopOut = new ContentPopOut(context::getHost);


        expanderUI = new EntryUI<>(
                getRootExpander(),
                contentPopOut,
                new ReferenceStore(),
                getRootExpander().getRegistration()
        );

        Flow container = new Flow();
        container.add(expanderUI.childrenLayout);
        root.add(container);
        Scrollbar scrollbar = new Scrollbar();
        root.add(scrollbar).setColumn(1);
        Elements.makeVerticalContainerScrollable(container, scrollbar, true);

        additionPoint.accept(root);
    }

    @Override
    public void tick() {

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
