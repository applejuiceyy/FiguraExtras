package com.github.applejuiceyy.figuraextras.views.avatar.main;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
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
import net.minecraft.util.Tuple;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;

public class MainAvatarsView implements Lifecycle, View.ImplementsMeta {
    private final Button guiScaleButton;
    private final Grid.GridSettings guiScaleButtonSettings;
    private final View.Context<Void> context;
    private final Label avatarSelectedLabel;
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

        avatarSelectedLabel = new Label("No Avatar Selected");
        centerer.add(avatarSelectedLabel).setRow(1).setColumn(1);

        selectAvatarButton.activation.subscribe(event -> {
            event.cancelPropagation();
            //noinspection SuspiciousNameCombination
            Tuple<Double, Double> pos = event.motivation.map(
                    kv -> new Tuple<>((double) selectAvatarButton.x.get(), (double) selectAvatarButton.y.get() + selectAvatarButton.height.get()),
                    mv -> new Tuple<>(mv.x, mv.y)
            );
            Elements.spawnContextMenu(root.getState(), pos.getA(), pos.getB(), (flow, culler) -> {
                boolean empty = true;
                for (Avatar loadedAvatar : AvatarManager.getLoadedAvatars()) {
                    empty = false;
                    Component text = Component.literal(loadedAvatar.entityName).append(": ").append(loadedAvatar.name);
                    Button button = (Button) Button.minimal().addAnd(text);
                    button.activation.subscribe(r -> {
                        culler.run();
                        avatarSelectedLabel.setText(text);
                        container.setView(ensureAvatarLoaded((c1, c2) -> {
                            TabView<Avatar> tabView = new TabView<>(c1, c2);
                            tabView.add("Object View", ensureScript(ObjectView::new));
                            tabView.add("Model View", ensureScript(ModelView::new));
                            tabView.add(Elements.separator());
                            tabView.add("Tick Instructions", ensureScript((c, ap) -> new MetricsView(c, ap, c.getValue().tick)));
                            tabView.add("Render Instructions", ensureScript((c, ap) -> new MetricsView(c, ap, c.getValue().render)));
                            tabView.add(Elements.separator());
                            tabView.add("Textures", TextureView::new);
                            tabView.add("Sounds", SoundView::new);
                            tabView.add(Elements.separator());
                            tabView.add("Output", ChatLikeView::new);
                            tabView.add("Network", NetworkView::new);
                            tabView.add(Elements.separator());
                            tabView.add("Capturer", CaptureView::new);
                            tabView.add(Elements.separator());
                            tabView.add(Button.minimal().addAnd("Download Avatar"));
                            return tabView;
                        }), loadedAvatar);
                    });
                    flow.add(button);
                }
                if (empty) {
                    flow.add(Component.literal("No avatars loaded").setStyle(Style.EMPTY.withColor(0xffaaaaaa)));
                }
            });
        });


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

    private View.ViewConstructor<View.Context<Avatar>, ? extends Lifecycle> ensureAvatarLoaded(View.ViewConstructor<View.Context<Avatar>, ? extends Lifecycle> what) {
        return View.context()
                .predicate(avatar -> avatar.loaded && !((AvatarAccess) avatar).figuraExtrass$isCleaned())
                .ifTrue(what)
                .ifFalse("Avatar not loaded");
    }
}
