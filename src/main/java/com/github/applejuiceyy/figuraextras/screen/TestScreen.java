package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.GuiState;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TestScreen extends Screen {

    private final GuiState awWrapper;

    public TestScreen(Component title) {
        super(title);

        Grid root = new Grid();

        root.addRow(0, Grid.SpacingKind.CONTENT);
        root.addRow(1, Grid.SpacingKind.PERCENTAGE);
        root.addColumn(1, Grid.SpacingKind.PERCENTAGE);

        Grid top = new Grid();
        root.add(top).setColumn(0).setRow(0);

        top.setSurface(Surface.solid(0xff111111));
        top.rows().content().cols().content().percentage(1).content();

        Grid left = new Grid();
        top.add(left);
        left
                .rows()
                .content()
                .cols()
                .content()
                .content();

        Button selectAvatarButton = (Button) Button.minimal(2).addAnd("Select avatar");
        left.add(selectAvatarButton);

        Grid centerer = new Grid();
        left.add(centerer).setColumn(1);
        centerer
                .rows()
                .percentage(1)
                .content()
                .percentage(1)
                .cols()
                .fixed(10)
                .content();

        centerer.add("No Avatar Selected").setRow(1).setColumn(1);

        Button guiScaleButton = (Button) Button.minimal(2).addAnd("Gui Scale: Auto");
        top.add(guiScaleButton).setColumn(2);

        Grid bottom = new Grid();
        root.add(bottom).setColumn(0).setRow(1);
        bottom.rows()
                .percentage(1)
                .cols()
                .content()
                .percentage(1);
        bottom.setSurface(Surface.solid(0xff050505));

        Grid flowRootRoot = new Grid();
        bottom.add(flowRootRoot);
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

        Flow flow = new Flow();
        flowRoot.add(flow).setColumn(1).setRow(1);

        flow.add(Button.minimal().addAnd("Object View"));
        flow.add(Button.minimal().addAnd("Model View"));
        flow.add(generateSeparator());
        flow.add(Button.minimal().addAnd("Tick Instructions"));
        flow.add(Button.minimal().addAnd("Render Instructions"));
        flow.add(generateSeparator());
        flow.add(Button.minimal().addAnd("Loaded Textures"));
        flow.add(Button.minimal().addAnd("Loaded Sounds"));
        flow.add(generateSeparator());
        flow.add(Button.minimal().addAnd("Output"));
        flow.add(Button.minimal().addAnd("Network"));
        flow.add(generateSeparator());
        flow.add(Button.minimal().addAnd("Capture"));
        flow.add(generateSeparator());
        flow.add(Button.minimal().addAnd("Download Avatar"));

        Scrollbar scrollbar = new Scrollbar();
        flowRoot.add(scrollbar).setColumn(2).setRow(1).setOptimalWidth(false).setWidth(5);

        Elements.makeVerticalContainerScrollable(flow, scrollbar, true);

        flowRoot.setSurface(Surface.contextBackground());

        awWrapper = root.getState();
    }

    private Grid generateSeparator() {
        Grid grid = new Grid();
        grid.rows()
                .fixed(2)
                .fixed(1)
                .fixed(2)
                .cols()
                .percentage(1)
                .percentage(8)
                .percentage(1);
        grid.add(new Grid().setSurface(Surface.solid(0xff444444))).setRow(1).setColumn(1);
        return grid;
    }

    @Override
    protected void init() {
        addRenderableWidget(awWrapper);
        awWrapper.setWidth(width);
        awWrapper.setHeight(height);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.getChildAt(mouseX, mouseY).ifPresent(element -> element.mouseMoved(mouseX, mouseY));
    }
}
