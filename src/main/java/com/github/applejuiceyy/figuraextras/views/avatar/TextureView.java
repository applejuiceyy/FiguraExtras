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
import com.github.applejuiceyy.figuraextras.util.Differential;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.View;
import com.google.common.collect.Iterators;
import net.minecraft.ChatFormatting;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;

import java.util.HashMap;

public class TextureView implements Lifecycle {
    private final Flow layout;
    private final View.Context<Avatar> context;

    private final HashMap<FiguraTexture, Instance> textures = new HashMap<>();
    private final Differential<FiguraTexture, FiguraTexture, Instance> differential;

    public TextureView(View.Context<Avatar> context, ParentElement.AdditionPoint additionPoint) {
        this.context = context;

        layout = new Flow();
        additionPoint.accept(Elements.withVerticalScroll(layout));

        differential = new Differential<>(
            () -> Iterators.concat(context.getValue().renderer.textures.values().iterator(), context.getValue().renderer.customTextures.values().iterator()),
            tex -> tex,
            tex -> {
                Instance inst = new Instance(tex, context);
                layout.add(inst.root);
                textures.put(tex, inst);
                return inst;
            },
            Instance::dispose
        );
    }

    @Override
    public void tick() {
        differential.update(Instance::tick);
    }

    @Override
    public void render() {
        differential.update(Instance::render);
    }

    @Override
    public void dispose() {
        differential.dispose();
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
        public Grid root;
        private boolean showingUpdatedTexture = false;

        public Instance(FiguraTexture texture, View.Context<Avatar> context) {
            notDirty = net.minecraft.network.chat.Component.literal(texture.getName() + "    ");
            dirty = net.minecraft.network.chat.Component.literal(texture.getName() + "*    ").withStyle(ChatFormatting.GOLD);
            this.texture = texture;

            root = new Grid();
            root.rows().fixed(2).percentage(1).fixed(2).cols().fixed(2).percentage(1).fixed(2);
            root.setSurface(Surface.contextBackground());

            Grid rootContent = new Grid();
            rootContent.rows().content().content().content().cols().percentage(1);
            root.add(rootContent).setRow(1).setColumn(1);

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

            rootContent.add(nomenclatureLayout);
            rootContent.add(new Label(net.minecraft.network.chat.Component.literal("   (" + texture.getWidth() + "x" + texture.getHeight() + ")").withStyle(ChatFormatting.GRAY)))
                .setRow(1);

            figuraTextureComponent = new FiguraTextureComponent(texture, c -> {
                if (this.showingUpdatedTexture) {
                    ((FiguraTextureAccess) texture).figuraExtras$refreshUpdatedTexture();
                    return ((FiguraTextureAccess) texture).figuraExtras$getUpdatedTexture();
                } else {
                    return texture.getLocation();
                }
            }, context.getValue());

            Grid textureContainer = new Grid();
            textureContainer.rows().content().cols().percentage(1).content().percentage(1);
            textureContainer.add(figuraTextureComponent).setColumn(1);
            rootContent.add(Elements.withHorizontalScroll(textureContainer, true)).setRow(2);
        }

        private void setShowingUpdatedTexture(boolean showing) {
            if (showingUpdatedTexture != showing) {
                if (showing) {
                    ((FiguraTextureAccess) texture).figuraExtras$lockUpdatedTexture();
                } else {
                    ((FiguraTextureAccess) texture).figuraExtras$unlockUpdatedTexture();
                }
                figuraTextureComponent.enqueueDirtySection(false, false);
                showingUpdatedTexture = showing;
            }
        }

        public void render() {
            figuraTextureComponent.enqueueDirtySection(false, false);
        }

        public void tick() {
            boolean modifications = ((FiguraTextureAccess) texture).figuraExtras$hasPendingModifications();
            label.setText(modifications ? dirty : notDirty);
            Grid.GridSettings settings = nomenclatureLayout.getSettings(button);
            settings.setInvisible(!modifications);
            settings.setDoLayout(modifications);
        }

        public void dispose() {
            setShowingUpdatedTexture(false);
            root.getParent().remove(root);
        }
    }
}