package com.github.applejuiceyy.figuraextras.views.avatar.main;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.TabView;
import com.github.applejuiceyy.figuraextras.views.View;
import com.github.applejuiceyy.figuraextras.views.ViewContainer;
import com.github.applejuiceyy.figuraextras.views.avatar.*;
import com.github.applejuiceyy.figuraextras.views.avatar.http.NetworkView;
import com.github.applejuiceyy.figuraextras.window.WindowContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;

public class MainAvatarsView implements Lifecycle, View.ImplementsMeta {
    private final Button guiScaleButton;
    private final Grid.GridSettings guiScaleButtonSettings;
    private final View.Context<Void> context;
    ViewContainer container;

    public MainAvatarsView(View.Context<Void> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;

        Grid root = new Grid();
        additionPoint.accept(root);

        root.setSurface(Surface.solid(0xff000000));

        root.addRow(0, Grid.SpacingKind.CONTENT);
        root.addRow(1, Grid.SpacingKind.PERCENTAGE);
        root.addColumn(1, Grid.SpacingKind.PERCENTAGE);

        Grid top = new Grid();
        root.add(top).setColumn(0).setRow(0);

        top.setSurface(Surface.solid(0xff111111));
        top.rows().content().cols().content().percentage(1).content();

        container = new ViewContainer(context::getWindowContext, root.adder(settings -> settings.setColumn(0).setRow(1)));

        Grid left = new Grid();
        top.add(left);
        left
                .rows()
                .content()
                .cols()
                .content()
                .content();

        Button selectAvatarButton = (Button) Button.minimal(2).addAnd("Select avatar");
        selectAvatarButton.mouseDown.subscribe(event -> {
            event.cancelPropagation();
            Elements.spawnContextMenu(root.getState(), event.x, event.y, (flow, culler) -> {
                boolean empty = true;
                for (Avatar loadedAvatar : AvatarManager.getLoadedAvatars()) {
                    empty = false;
                    Component text = Component.literal(loadedAvatar.entityName).append(": ").append(loadedAvatar.name);
                    Button button = (Button) Button.minimal().addAnd(text);
                    button.activation.subscribe(r -> {
                        culler.run();
                        TabView tabView = container.setView(TabView::new, null);
                        tabView.add("Object View", ensureScript(ObjectView::new), loadedAvatar);
                        tabView.add("Model View", ensureScript(ModelView::new), loadedAvatar);
                        tabView.add(Elements.separator());
                        tabView.add("Tick Instructions", ensureScript((c, ap) -> new MetricsView(c, ap, c.getValue().tick)), loadedAvatar);
                        tabView.add("Render Instructions", ensureScript((c, ap) -> new MetricsView(c, ap, c.getValue().render)), loadedAvatar);
                        tabView.add(Elements.separator());
                        tabView.add("Textures", TextureView::new, loadedAvatar);
                        tabView.add("Sounds", SoundView::new, loadedAvatar);
                        tabView.add(Elements.separator());
                        tabView.add("Output", ChatLikeView::new, loadedAvatar);
                        tabView.add("Network", NetworkView::new, loadedAvatar);
                        tabView.add(Elements.separator());
                        tabView.add("Capturer", CaptureView::new, loadedAvatar);
                        tabView.add(Elements.separator());
                        tabView.add(Button.minimal().addAnd("Download Avatar"));
                    });
                    flow.add(button);
                }
                if (empty) {
                    flow.add(Component.literal("No avatars loaded").setStyle(Style.EMPTY.withColor(0xffaaaaaa)));
                }
            });
        });
        left.add(selectAvatarButton);

        Grid centerer = new Grid();
        left.add(centerer).setColumn(1);
        centerer.rows()
                .percentage(1)
                .content()
                .percentage(1)
                .cols()
                .fixed(10)
                .content();

        centerer.add("No Avatar Selected").setRow(1).setColumn(1);

        this.guiScaleButton = (Button) Button.minimal(2).addAnd("Gui Scale: Auto");
        guiScaleButtonSettings = top.add(guiScaleButton);
        guiScaleButtonSettings.setColumn(2).setDoLayout(false).setInvisible(true);
    }

    @Override
    public void tick() {
        container.tick();
    }

    @Override
    public void render() {
        container.render();
    }

    @Override
    public void dispose() {
        container.dispose();
    }

    @Override
    public void enableMeta() {
        guiScaleButtonSettings.setDoLayout(true).setInvisible(false);
        guiScaleButton.activation.subscribe(event -> {
            WindowContext windowContext = context.getWindowContext();
            int current = windowContext.getLockedGuiScale().orElse(0);
            current++;
            if (current >= windowContext.getRecommendedGuiScale()) {
                windowContext.unlockGuiScale();
                guiScaleButton.setText(Component.literal("Gui Scale: AUTO"));
            } else {
                windowContext.lockGuiScale(current);
                guiScaleButton.setText(Component.literal("Gui Scale: " + current));
            }
        });
    }

    private View.ViewConstructor<View.Context<Avatar>, ? extends Lifecycle> ensureScript(View.ViewConstructor<View.Context<Avatar>, ? extends Lifecycle> what) {
        return View.context()
                .predicate(avatar -> avatar.loaded && avatar.luaRuntime != null)
                .ifTrue(what)
                .ifFalse("Script not detected");
    }
}
