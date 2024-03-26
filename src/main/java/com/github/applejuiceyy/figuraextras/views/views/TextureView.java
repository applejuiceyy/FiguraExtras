package com.github.applejuiceyy.figuraextras.views.views;

import com.github.applejuiceyy.figuraextras.components.FiguraTextureComponent;
import com.github.applejuiceyy.figuraextras.ducks.FiguraTextureAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Scrollbar;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import net.minecraft.ChatFormatting;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TextureView implements InfoViews.View {
    private final Flow layout;
    private final Grid root;
    private final InfoViews.Context context;

    private final HashMap<FiguraTexture, Instance> textures = new HashMap<>();

    public TextureView(InfoViews.Context context) {
        this.context = context;

        root = new Grid();
        root.rows()
                .content()
                .cols()
                .content()
                .content();

        layout = new Flow();
        Scrollbar scrollbar = new Scrollbar();
        Elements.makeVerticalContainerScrollable(layout, scrollbar, true);
    }

    @Override
    public void tick() {
        ArrayList<FiguraTexture> seen = new ArrayList<>();
        for (FiguraTexture texture : context.getAvatar().renderer.textures.values()) {
            if (!textures.containsKey(texture)) {
                Instance inst = new Instance(texture, context);
                layout.add(inst.root);
                textures.put(texture, inst);
                seen.add(texture);
            }
            seen.add(texture);
        }

        for (FiguraTexture texture : context.getAvatar().renderer.customTextures.values()) {
            if (!textures.containsKey(texture)) {
                Instance inst = new Instance(texture, context);
                layout.add(inst.root);
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
    public Element getRoot() {
        return root;
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
        private final Label label;
        private final Button button;
        public Flow root;
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

            root = new Flow();
            root.setSurface(Surface.contextBackground());

            label = (Label) new Label().setText(texture.getName());
            Grid nomenclatureLayout = new Grid();
            nomenclatureLayout.rows()
                    .content()
                    .cols()
                    .content()
                    .content();

            button = (Button) Button.minimal().addAnd(showUpdatedTexture);
            button.activation.subscribe(event -> {
                setShowingUpdatedTexture(!showingUpdatedTexture);
                button.setText(showingUpdatedTexture ? showUploadedTexture : showUpdatedTexture);
            });

            nomenclatureLayout.add(label);
            nomenclatureLayout.add(button).setColumn(1);

            root.add(nomenclatureLayout);
            root.add(new Label(net.minecraft.network.chat.Component.literal("   (" + texture.getWidth() + "x" + texture.getHeight() + ")").withStyle(ChatFormatting.GRAY)));

            FiguraTextureComponent component = new FiguraTextureComponent(texture, () -> {
                if (this.showingUpdatedTexture) {
                    ((FiguraTextureAccess) texture).figuraExtrass$refreshUpdatedTexture();
                    return ((FiguraTextureAccess) texture).figuraExtrass$getUpdatedTexture();
                } else {
                    return texture.getLocation();
                }
            }, context.getAvatar());


            root.add(component);
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
            label.setText(modifications ? dirty : notDirty);
        }

        public void dispose() {
            if (showingUpdatedTexture) {
                ((FiguraTextureAccess) texture).figuraExtrass$lockUpdatedTexture();
                showingUpdatedTexture = false;
            }
            root.getParent().remove(root);
        }
    }
}