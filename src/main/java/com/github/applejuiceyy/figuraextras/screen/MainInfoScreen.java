package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.GuiState;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.window.WindowContext;
import com.github.applejuiceyy.figuraextras.window.WindowContextReceiver;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.jetbrains.annotations.Nullable;

public class MainInfoScreen extends Screen implements WindowContextReceiver {
    private final GuiState awWrapper;
    private final Button guiScaleButton;
    private @Nullable AvatarInfoDisplay info = null;
    WindowContext context;
    AvatarInfoDisplay avatarInfoDisplay = null;

    boolean windowIsActive = true;

    float alpha = 1;

    public MainInfoScreen() {
        super(Component.empty());

        Grid root = new Grid();

        awWrapper = new GuiState(root);
        root.setSurface(Surface.solid(0xff000000));

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
        selectAvatarButton.mouseDown.subscribe(event -> {
            event.cancelPropagation();
            Elements.spawnContextMenu(awWrapper, event.x, event.y, (flow, culler) -> {
                boolean empty = true;
                for (Avatar loadedAvatar : AvatarManager.getLoadedAvatars()) {
                    empty = false;
                    Component text = Component.literal(loadedAvatar.entityName).append(": ").append(loadedAvatar.name);
                    Button button = (Button) Button.minimal().addAnd(text);
                    button.activation.subscribe(r -> {
                        culler.run();
                        if (info != null) {
                            info.dispose();
                            root.remove(info.root);
                        }
                        info = new AvatarInfoDisplay(new InfoViews.Context() {
                            @Override
                            public MainInfoScreen getScreen() {
                                return null;
                            }

                            @Override
                            public void setView(InfoViews.ViewConstructor<InfoViews.Context, Lifecycle> view) {
                                info.setView(view);
                            }

                            @Override
                            public Avatar getAvatar() {
                                return loadedAvatar;
                            }

                            @Override
                            public ParentElement<?> getRoot() {
                                return root;
                            }

                            @Override
                            public WindowContentPopOutHost getHost() {
                                return context.getContentPopOutHost();
                            }
                        });
                        root.add(info.root).setColumn(0).setRow(1);
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
        top.add(guiScaleButton).setColumn(2).setDoLayout(false).setInvisible(true);
    }

    public void tick() {
        if (info != null) {
            info.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (info != null) {
            info.render();
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    public void dispose() {
        if (info != null) {
            info.dispose();
        }
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

    @Override
    public void acknowledge(WindowContext context) {
        this.context = context;
        if (context.canSetGuiScale()) {
            ParentElement.Settings settings = guiScaleButton.getParent().getSettings(guiScaleButton);
            settings.setInvisible(false);
            settings.setDoLayout(true);
            guiScaleButton.activation.subscribe(event -> {
                int current = context.getLockedGuiScale().orElse(0);
                current++;
                if (current >= context.getRecommendedGuiScale()) {
                    context.unlockGuiScale();
                    guiScaleButton.setText(Component.literal("Gui Scale: AUTO"));
                } else {
                    context.lockGuiScale(current);
                    guiScaleButton.setText(Component.literal("Gui Scale: " + current));
                }
            });
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (avatarInfoDisplay != null) {
            avatarInfoDisplay.dispose();
        }
        awWrapper.dispose();
    }

    @Override
    public void windowActive(boolean focused) {
        windowIsActive = focused;
    }

    @Override
    public boolean testTransparency(Double x, Double y) {
        return true;
    }
}
