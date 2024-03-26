package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.views.views.ModelView;
import com.github.applejuiceyy.figuraextras.views.views.ObjectView;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class AvatarInfoDisplay {
    public final Grid root;
    private final InfoViews.Context context;

    private @Nullable InfoViews.View view = null;

    public AvatarInfoDisplay(InfoViews.Context context) {
        this.context = context;

        root = new Grid();
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

        Flow flow = new Flow();
        flowRoot.add(flow).setColumn(1).setRow(1);

        flow.add(hook(Button.minimal().addAnd("Object View"), ensureScript(ObjectView::new)));
        flow.add(hook(Button.minimal().addAnd("Model View"), ensureScript(ModelView::new)));
        flow.add(Elements.separator());
        flow.add(Button.minimal().addAnd("Tick Instructions"));
        flow.add(Button.minimal().addAnd("Render Instructions"));
        flow.add(Elements.separator());
        flow.add(Button.minimal().addAnd("Loaded Textures"));
        flow.add(Button.minimal().addAnd("Loaded Sounds"));
        flow.add(Elements.separator());
        flow.add(Button.minimal().addAnd("Output"));
        flow.add(Button.minimal().addAnd("Network"));
        flow.add(Elements.separator());
        flow.add(Button.minimal().addAnd("Capture"));
        flow.add(Elements.separator());
        flow.add(Button.minimal().addAnd("Download Avatar"));

        Scrollbar scrollbar = new Scrollbar();
        flowRoot.add(scrollbar).setColumn(2).setRow(1).setOptimalWidth(false).setWidth(5);

        Elements.makeVerticalContainerScrollable(flow, scrollbar, true);

        flowRoot.setSurface(Surface.contextBackground());
    }

    private Function<InfoViews.Context, ? extends InfoViews.View> ensureScript(Function<InfoViews.Context, ? extends InfoViews.View> what) {
        return InfoViews.onlyIf(avatar -> avatar.loaded && avatar.luaRuntime != null, what, Component.literal("Script not detected"));
    }

    private Element hook(Element button, Runnable toRun) {
        button.activation.subscribe(event -> toRun.run());
        return button;
    }

    private Element hook(Element button, Function<InfoViews.Context, ? extends InfoViews.View> view) {
        Function<InfoViews.Context, ? extends InfoViews.View> actual = wrap(view);
        return hook(button, () -> switchToView(actual));
    }

    private void switchToView(Function<InfoViews.Context, ? extends InfoViews.View> apply) {
        InfoViews.View built = apply.apply(context);
        if (view != null) {
            view.dispose();
            root.remove(built.getRoot());
        }

        view = built;

        root.add(built.getRoot()).setColumn(1);
    }

    public void setView(Function<InfoViews.Context, InfoViews.View> view) {
        switchToView(wrap(view));
    }

    private Function<InfoViews.Context, ? extends InfoViews.View> wrap(Function<InfoViews.Context, ? extends InfoViews.View> v) {
        return InfoViews.onlyIf(
                avatar -> Minecraft.getInstance().level != null,
                InfoViews.onlyIf(
                        avatar -> !((AvatarAccess) avatar).figuraExtrass$isCleaned(),
                        v,
                        Component.literal("Avatar has been cleared")
                ),
                Component.literal("World not loaded")
        );
    }

    public void tick() {
        if (view != null) {
            view.tick();
        }
    }

    public void render() {
        if (view != null) {
            view.render();
        }
    }

    public void dispose() {
        if (view != null) {
            view.dispose();
        }
    }
}
