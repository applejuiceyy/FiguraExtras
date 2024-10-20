package com.github.applejuiceyy.figuraextras.views.avatar;

import com.github.applejuiceyy.figuraextras.components.FiguraTextureComponent;
import com.github.applejuiceyy.figuraextras.ducks.FiguraTextureAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import net.minecraft.ChatFormatting;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TextureView implements Lifecycle {
    private final Flow layout;
    private final View.Context<Avatar> context;

    private final HashMap<FiguraTexture, Instance> textures = new HashMap<>();

    public TextureView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;

        layout = new Flow();
        additionPoint.accept(Elements.withVerticalScroll(layout));
    }

    @Override
    public void tick() {
        ArrayList<FiguraTexture> seen = new ArrayList<>();
        for (FiguraTexture texture : context.getValue().renderer.textures.values()) {
            if (!textures.containsKey(texture)) {
                Instance inst = new Instance(texture, context);
                layout.add(inst.root);
                textures.put(texture, inst);
                seen.add(texture);
            }
            seen.add(texture);
        }

        for (FiguraTexture texture : context.getValue().renderer.customTextures.values()) {
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
    public void render() {
        for (Instance value : textures.values()) {
            value.render();
        }
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
        private final FiguraTextureComponent figuraTextureComponent;
        private final Grid nomenclatureLayout;
        private final net.minecraft.network.chat.Component notDirty;
        private final net.minecraft.network.chat.Component dirty;
        private final net.minecraft.network.chat.Component showUpdatedTexture =
                net.minecraft.network.chat.Component.literal("Showing Uploaded Texture").withStyle(ChatFormatting.UNDERLINE);
        private final net.minecraft.network.chat.Component showUploadedTexture =
                net.minecraft.network.chat.Component.literal("Showing Updated Texture").withStyle(ChatFormatting.UNDERLINE);
        public Flow root;
        private boolean showingUpdatedTexture = false;

        public Instance(FiguraTexture texture, View.Context<Avatar> context) {
            notDirty = net.minecraft.network.chat.Component.literal(texture.getName() + "    ");
            dirty = net.minecraft.network.chat.Component.literal(texture.getName() + "*    ").withStyle(ChatFormatting.GOLD);
            this.texture = texture;

            root = new Flow();
            root.setSurface(Surface.contextBackground());

            label = (Label) new Label().setText(texture.getName());
            this.nomenclatureLayout = new Grid();
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

            figuraTextureComponent = new FiguraTextureComponent(texture, c -> {
                if (this.showingUpdatedTexture) {
                    ((FiguraTextureAccess) texture).figuraExtrass$refreshUpdatedTexture();
                    return ((FiguraTextureAccess) texture).figuraExtrass$getUpdatedTexture();
                } else {
                    return texture.getLocation();
                }
            }, context.getValue());

            root.add(Elements.withHorizontalScroll(new Flow().addAnd(figuraTextureComponent), true));
        }

        private void setShowingUpdatedTexture(boolean showing) {
            if (showingUpdatedTexture != showing) {
                if (showing) {
                    ((FiguraTextureAccess) texture).figuraExtrass$lockUpdatedTexture();
                } else {
                    ((FiguraTextureAccess) texture).figuraExtrass$unlockUpdatedTexture();
                }
                figuraTextureComponent.enqueueDirtySection(false, false);
                showingUpdatedTexture = showing;
            }
        }

        public void render() {
            figuraTextureComponent.enqueueDirtySection(false, false);
        }

        public void tick() {
            boolean modifications = ((FiguraTextureAccess) texture).figuraExtrass$hasPendingModifications();
            label.setText(modifications ? dirty : notDirty);
            nomenclatureLayout.getSettings(button).setInvisible(!modifications);
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