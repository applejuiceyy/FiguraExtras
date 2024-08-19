package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class TabView<DN> implements Lifecycle {
    public final Grid root;
    private final Flow flow;
    private final ViewContainer container;
    private final View.Context<DN> context;

    public TabView(View.Context<DN> context, ParentElement.AdditionPoint additionPoint) {
        root = new Grid();
        this.context = context;
        additionPoint.accept(root);
        container = new ViewContainer(context::getWindowContext, root.adder(settings -> settings.setColumn(1)));
        root.rows()
                .percentage(1)
                .cols()
                .content()
                .percentage(1);

        Grid flowRootRoot = new Grid();
        root.add(flowRootRoot);
        flowRootRoot.rows()
                .content()
                .percentage(1)
                .cols()
                .content();

        Grid flowRoot = new Grid();
        flowRootRoot.add(flowRoot);
        flowRoot.rows()
                .fixed(2)
                .percentage(1)
                .fixed(2)
                .cols()
                .fixed(4)
                .content()
                .content()
                .fixed(4);

        flow = new Flow();
        flowRoot.add(flow).setColumn(1).setRow(1);


        Scrollbar scrollbar = new Scrollbar();
        flowRoot.add(scrollbar).setColumn(2).setRow(1).setOptimalWidth(false).setWidth(5);

        Elements.makeVerticalContainerScrollable(flow, scrollbar, true);

        flowRoot.setSurface(Surface.contextBackground());
    }

    public <N> void add(String text, View.ViewConstructor<View.Context<N>, ?> constructor, N thing) {
        add(Component.literal(text), a -> a.setView(constructor, thing));
    }

    public void add(String text, View.ViewConstructor<View.Context<DN>, ?> constructor) {
        add(Component.literal(text), a -> a.setView(constructor, context.getValue()));
    }

    public void add(String text, Consumer<SetViewCallback> activation) {
        add(Component.literal(text), activation);
    }

    public void add(Component text, Consumer<SetViewCallback> activation) {
        ParentElement<Grid.GridSettings> element = Button.minimal().addAnd(text);
        element.activation.getSource().subscribe(event -> activation.accept(container::setView));
        add(element);
    }

    public void add(Element element) {
        flow.add(element);
    }


    public void tick() {
        container.tick();
    }

    public void render() {
        container.render();
    }

    public void dispose() {
        container.dispose();
        ;
    }

    public interface SetViewCallback {
        <N> void setView(View.ViewConstructor<View.Context<N>, ?> constructor, N thing);
    }
}
