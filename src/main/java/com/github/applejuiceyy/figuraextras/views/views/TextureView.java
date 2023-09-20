package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.FiguraTextureComponent;
import com.github.applejuiceyy.figuraextras.components.SmallButtonComponent;
import com.github.applejuiceyy.figuraextras.ducks.FiguraTextureAccess;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.ChatFormatting;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TextureView implements InfoViews.View {
    private final FlowLayout layout;
    private final ScrollContainer<FlowLayout> scrollable;
    private final InfoViews.Context context;

    private final HashMap<FiguraTexture, Instance> textures = new HashMap<>();

    public TextureView(InfoViews.Context context) {
        this.context = context;
        layout = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        scrollable = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), layout);
    }

    @Override
    public void tick() {
        ArrayList<FiguraTexture> seen = new ArrayList<>();
        for (FiguraTexture texture : context.getAvatar().renderer.textures.values()) {
            if (!textures.containsKey(texture)) {
                Instance inst = new Instance(texture, context);
                layout.child(inst.root);
                textures.put(texture, inst);
                seen.add(texture);
            }
            seen.add(texture);
        }

        for (FiguraTexture texture : context.getAvatar().renderer.customTextures.values()) {
            if (!textures.containsKey(texture)) {
                Instance inst = new Instance(texture, context);
                layout.child(inst.root);
                textures.put(texture, inst);
            }
            seen.add(texture);
        }

        for (Iterator<Map.Entry<FiguraTexture, Instance>> iterator = textures.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<FiguraTexture, Instance> figuraTextureTextureComponentEntry = iterator.next();
            if (!seen.contains(figuraTextureTextureComponentEntry.getKey())) {
                iterator.remove();
                figuraTextureTextureComponentEntry.getValue().dispose();
            } else {
                figuraTextureTextureComponentEntry.getValue().tick();
            }
        }
    }

    @Override
    public Component getRoot() {
        return scrollable;
    }

    @Override
    public void render() {

    }

    @Override
    public void dispose() {
        for (Instance value : textures.values()) {
            value.dispose();
        }
    }

    static class Instance {

        private final FiguraTexture texture;
        public FlowLayout root;
        private final LabelComponent label;
        private final SmallButtonComponent button;
        private final net.minecraft.network.chat.Component notDirty;
        private final net.minecraft.network.chat.Component dirty;

        private final net.minecraft.network.chat.Component showUpdatedTexture =
                net.minecraft.network.chat.Component.literal("Show Updated Texture").withStyle(ChatFormatting.UNDERLINE);
        private final net.minecraft.network.chat.Component showUploadedTexture =
                net.minecraft.network.chat.Component.literal("Show Uploaded Texture").withStyle(ChatFormatting.UNDERLINE);


        private boolean showingUpdatedTexture = false;

        public Instance(FiguraTexture texture, InfoViews.Context context) {
            notDirty = net.minecraft.network.chat.Component.literal(texture.getName() + "    ");
            dirty = net.minecraft.network.chat.Component.literal(texture.getName() + "*    ").withStyle(ChatFormatting.GOLD);
            this.texture = texture;

            root = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            root.surface(Surface.TOOLTIP);
            root.padding(Insets.of(5));

            label = Components.label(net.minecraft.network.chat.Component.literal(texture.getName()));
            FlowLayout nomenclatureLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());

            button = new SmallButtonComponent(showUpdatedTexture, 0x00000000);
            button.mouseDown().subscribe((x, y, d) -> {
                setShowingUpdatedTexture(!showingUpdatedTexture);
                button.setText(showingUpdatedTexture ? showUploadedTexture : showUpdatedTexture);
                return true;
            });

            nomenclatureLayout.child(label);
            nomenclatureLayout.child(button);

            root.child(nomenclatureLayout);
            root.child(Components.label(net.minecraft.network.chat.Component.literal("   (" + texture.getWidth() + "x" + texture.getHeight() + ")").withStyle(ChatFormatting.GRAY)));

            FiguraTextureComponent component = new FiguraTextureComponent(texture, () -> {
                if (this.showingUpdatedTexture) {
                    ((FiguraTextureAccess) texture).figuraExtrass$refreshUpdatedTexture();
                    return ((FiguraTextureAccess) texture).figuraExtrass$getUpdatedTexture();
                } else {
                    return texture.getLocation();
                }
            }, context.getAvatar());

            FlowLayout textureLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            textureLayout.padding(Insets.bottom(10));
            textureLayout.child(component);

            ScrollContainer<FlowLayout> container = new ScrollContainer<>(ScrollContainer.ScrollDirection.HORIZONTAL, Sizing.fill(100), Sizing.content(), textureLayout) {
                @Override
                public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
                    return false;
                }
            };
            container.scrollbar(ScrollContainer.Scrollbar.vanilla());
            container.scrollbarThiccness(10);
            root.child(container);
        }

        private void setShowingUpdatedTexture(boolean showing) {
            if (showingUpdatedTexture != showing) {
                if (showing) {
                    ((FiguraTextureAccess) texture).figuraExtrass$lockUpdatedTexture();
                } else {
                    ((FiguraTextureAccess) texture).figuraExtrass$unlockUpdatedTexture();
                }
                showingUpdatedTexture = showing;
            }
        }

        public void tick() {
            boolean modifications = ((FiguraTextureAccess) texture).figuraExtrass$hasPendingModifications();
            label.text(modifications ? dirty : notDirty);
        }

        public void dispose() {
            if (showingUpdatedTexture) {
                ((FiguraTextureAccess) texture).figuraExtrass$lockUpdatedTexture();
                showingUpdatedTexture = false;
            }
            root.remove();
        }
    }
}