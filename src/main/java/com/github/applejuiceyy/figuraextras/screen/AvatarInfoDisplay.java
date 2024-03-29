package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.views.views.*;
import com.github.applejuiceyy.figuraextras.views.views.http.NetworkView;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public class AvatarInfoDisplay {
    public final Grid root;
    private final InfoViews.Context context;

    private final ParentElement.AdditionPoint additionPoint;

    private @Nullable Lifecycle view = null;

    public AvatarInfoDisplay(InfoViews.Context context) {
        this.context = context;

        root = new Grid();
        additionPoint = root.adder(settings -> settings.setColumn(1));
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
        flow.add(hook(Button.minimal().addAnd("Tick Instructions"), ensureScript((c, ap) -> new MetricsView(c, ap, c.getAvatar().tick))));
        flow.add(hook(Button.minimal().addAnd("Render Instructions"), ensureScript((c, ap) -> new MetricsView(c, ap, c.getAvatar().render))));
        flow.add(Elements.separator());
        flow.add(hook(Button.minimal().addAnd("Loaded Textures"), TextureView::new));
        flow.add(Button.minimal().addAnd("Loaded Sounds"));
        flow.add(Elements.separator());
        flow.add(Button.minimal().addAnd("Output"));
        flow.add(hook(Button.minimal().addAnd("Network"), NetworkView::new));
        flow.add(Elements.separator());
        flow.add(hook(Button.minimal().addAnd("Capture"), CaptureView::new));
        flow.add(Elements.separator());
        flow.add(Button.minimal().addAnd("Download Avatar"));

        Scrollbar scrollbar = new Scrollbar();
        flowRoot.add(scrollbar).setColumn(2).setRow(1).setOptimalWidth(false).setWidth(5);

        Elements.makeVerticalContainerScrollable(flow, scrollbar, true);

        flowRoot.setSurface(Surface.contextBackground());
    }

    private InfoViews.ViewConstructor<InfoViews.Context, ? extends Lifecycle> ensureScript(InfoViews.ViewConstructor<InfoViews.Context, ? extends Lifecycle> what) {
        return InfoViews.context()
                .predicate(avatar -> avatar.loaded && avatar.luaRuntime != null)
                .ifTrue(what)
                .ifFalse("Script not detected");
    }

    private Element hook(Element button, Runnable toRun) {
        button.activation.subscribe(event -> toRun.run());
        return button;
    }

    private Element hook(Element button, InfoViews.ViewConstructor<InfoViews.Context, ? extends Lifecycle> view) {
        InfoViews.ViewConstructor<InfoViews.Context, ? extends Lifecycle> actual = wrap(view);
        return hook(button, () -> switchToView(actual));
    }

    private void switchToView(InfoViews.ViewConstructor<InfoViews.Context, ? extends Lifecycle> apply) {
        if (view != null) {
            view.dispose();
            additionPoint.remove();
        }

        view = apply.apply(context, additionPoint);
    }

    public void setView(InfoViews.ViewConstructor<InfoViews.Context, Lifecycle> view) {
        switchToView(wrap(view));
    }

    private InfoViews.ViewConstructor<InfoViews.Context, ? extends Lifecycle> wrap(InfoViews.ViewConstructor<InfoViews.Context, ? extends Lifecycle> v) {
        return InfoViews.context()
                .predicate(() -> Minecraft.getInstance().level != null)
                .ifTrue(
                        InfoViews.context()
                                .predicate(avatar -> !((AvatarAccess) avatar).figuraExtrass$isCleaned())
                                .ifTrue(v)
                                .ifFalse("Avatar has been cleared")
                )
                .ifFalse("World not Loaded");
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
            additionPoint.remove();
        }
        additionPoint.ensureRemoved();
    }
}
