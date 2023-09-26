package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.ducks.AvatarAccess;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.views.views.*;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

public class AvatarInfoDisplay {
    public final FlowLayout root;
    private final InfoViews.Context context;
    private InfoViews.View view = null;
    DropdownComponent dropdownComponent;
    FlowLayout mountingPoint;

    AvatarInfoDisplay(InfoViews.Context context) {
        this.context = context;
        BaseUIModelScreen.DataSource source = BaseUIModelScreen.DataSource.asset(
                new ResourceLocation("figuraextras", "avatar_debug_screen")
        );

        root = source.get().createAdapterWithoutScreen(0, 0, 0, 0, FlowLayout.class).rootComponent;

        dropdownComponent = root.childById(DropdownComponent.class, "view-modes");
        mountingPoint = root.childById(FlowLayout.class, "view-mounting-point");


        addView(Component.literal("Object View"),
                InfoViews.onlyIf(avatar -> avatar.loaded && avatar.luaRuntime != null, ObjectView::new, Component.literal("Script not detected"))
        );

        addView(Component.literal("Model View"),
                InfoViews.onlyIf(avatar -> avatar.loaded && avatar.luaRuntime != null, ModelView::new, Component.literal("Script not detected"))
        );

        dropdownComponent.divider();

        addView(Component.literal("Tick Instructions"),
                InfoViews.onlyIf(avatar -> avatar.loaded && avatar.luaRuntime != null, c -> new MetricsView(context, c.getAvatar().tick), Component.literal("Script not detected"))
        );

        addView(Component.literal("Render Instructions"),
                InfoViews.onlyIf(avatar -> avatar.loaded && avatar.luaRuntime != null, c -> new MetricsView(context, c.getAvatar().render), Component.literal("Script not detected"))
        );

        dropdownComponent.divider();

        addView(Component.literal("Loaded textures"), TextureView::new);

        addView(Component.literal("Loaded sounds"), SoundView::new);

        dropdownComponent.divider();

        addView(Component.literal("Output"), ChatLikeView::new);

        dropdownComponent.divider();

        addView(Component.literal("Capture"), InfoViews.onlyIf(avatar -> avatar.loaded && avatar.luaRuntime != null, CaptureView::new, Component.literal("Script not detected")));

        dropdownComponent.divider();

        dropdownComponent.button(Component.literal("Download Avatar"), ignored -> {
            try {
                Util.getPlatform().openUri(new URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            } catch (URISyntaxException ignored1) {
            }
        });
    }

    public void addView(Component text, Function<InfoViews.Context, InfoViews.View> v) {
        Function<InfoViews.Context, InfoViews.View> actual = wrap(v);
        dropdownComponent.button(text, o -> switchToView(actual.apply(context)));
    }

    private Function<InfoViews.Context, InfoViews.View> wrap(Function<InfoViews.Context, InfoViews.View> v) {
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

    private void switchToView(InfoViews.View apply) {
        if (view != null) {
            view.dispose();
            view.getRoot().remove();
        }

        view = apply;

        mountingPoint.child(view.getRoot());
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
        view.dispose();
    }

    public void setView(Function<InfoViews.Context, InfoViews.View> view) {
        switchToView(wrap(view).apply(context));
    }
}
