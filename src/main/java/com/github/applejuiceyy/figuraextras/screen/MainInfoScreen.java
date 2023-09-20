package com.github.applejuiceyy.figuraextras.screen;

import com.github.applejuiceyy.figuraextras.mixin.gui.owolib.OwoUIAdapterAccessor;
import com.github.applejuiceyy.figuraextras.screen.contentpopout.WindowContentPopOutHost;
import com.github.applejuiceyy.figuraextras.views.InfoViews;
import com.github.applejuiceyy.figuraextras.window.WindowContext;
import com.github.applejuiceyy.figuraextras.window.WindowContextReceiver;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;

import java.util.Objects;

public class MainInfoScreen extends BaseUIModelScreen<FlowLayout> implements WindowContextReceiver {
    ButtonComponent guiScale;
    ButtonComponent isolateComponent;
    LabelComponent currentAvatar;
    WindowContext context;
    AvatarInfoDisplay avatarInfoDisplay = null;
    WidgetIsolator isolator;

    boolean windowIsActive = true;

    float alpha = 1;

    public MainInfoScreen() {
        super(FlowLayout.class, DataSource.asset(new ResourceLocation("figuraextras", "debug_screen")));
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        isolator = new WidgetIsolator(uiAdapter.rootComponent) {
            @Override
            void setStyle(CursorStyle style) {
                ((OwoUIAdapterAccessor) uiAdapter).getCursorAdapter().applyStyle(style);
            }
        };

        ButtonComponent button = rootComponent.childById(ButtonComponent.class, "avatar-chooser");
        guiScale = rootComponent.childById(ButtonComponent.class, "toggle-gui-scale");
        currentAvatar = rootComponent.childById(LabelComponent.class, "current-avatar-name");
        isolateComponent = rootComponent.childById(ButtonComponent.class, "isolate-dropdownComponent");

        if (button != null) {
            button.onPress(ignored -> {
                DropdownComponent.openContextMenu(
                        this, rootComponent, FlowLayout::child, button.x(), button.y() + button.height(),
                        dropdown -> {
                            boolean none = true;
                            for (Avatar loadedAvatar : AvatarManager.getLoadedAvatars()) {
                                none = false;
                                Component text = Component.literal(loadedAvatar.entityName).append(": ").append(loadedAvatar.name);
                                dropdown.button(text, drop -> {
                                    if (avatarInfoDisplay != null) {
                                        avatarInfoDisplay.root.remove();
                                    }
                                    currentAvatar.text(text);
                                    avatarInfoDisplay = new AvatarInfoDisplay(new InfoViews.Context() {
                                        @Override
                                        public MainInfoScreen getScreen() {
                                            return MainInfoScreen.this;
                                        }

                                        @Override
                                        public Avatar getAvatar() {
                                            return loadedAvatar;
                                        }

                                        @Override
                                        public FlowLayout getRoot() {
                                            return uiAdapter.rootComponent;
                                        }

                                        @Override
                                        public WindowContentPopOutHost getHost() {
                                            return context.getContentPopOutHost();
                                        }
                                    });
                                    Objects.requireNonNull(rootComponent.childById(FlowLayout.class, "avatar-mounting-point"))
                                            .child(avatarInfoDisplay.root);
                                    drop.remove();
                                });
                            }

                            if (none) {
                                dropdown.text(Component.literal("No avatars loaded"));
                            }
                        });
            });
        }

        if (guiScale != null) {
            guiScale.visible = false;
        }
        if (isolateComponent != null) {
            isolateComponent.visible = false;
        }
    }

    @Override
    public void acknowledge(WindowContext context) {
        this.context = context;
        if (guiScale != null && context.canSetGuiScale()) {
            guiScale.visible = true;
            guiScale.onPress(o -> {
                int current = context.getLockedGuiScale().orElse(0);
                current++;
                if (current >= context.getRecommendedGuiScale()) {
                    context.unlockGuiScale();
                    o.setMessage(Component.literal("Gui Scale: AUTO"));
                } else {
                    context.lockGuiScale(current);
                    o.setMessage(Component.literal("Gui Scale: " + current));
                }
            });
        }
        if (isolateComponent != null) {
            isolateComponent.visible = true;
            isolateComponent.onPress(o -> isolator.startIsolating());
        }
    }

    private boolean scrapeClassForMouseFocus(Class<?> cls) {
        try {
            Class<?> declaringClass = cls.getMethod("onMouseDown", double.class, double.class, int.class).getDeclaringClass();
            if (testClass(declaringClass)) {
                return true;
            }
            declaringClass = cls.getMethod("onMouseScroll", double.class, double.class, double.class).getDeclaringClass();
            if (testClass(declaringClass)) {
                return true;
            }
        } catch (NoSuchMethodException e) {
            return false;
        }

        return false;
    }

    private boolean testClass(Class<?> declaringClass) {
        return declaringClass != io.wispforest.owo.ui.core.Component.class &&
                declaringClass != io.wispforest.owo.ui.base.BaseComponent.class &&
                declaringClass != io.wispforest.owo.ui.base.BaseParentComponent.class;
    }

    @Override
    public void tick() {
        if (context == null || !context.isCompletelyOverlaying()) {
            alpha = Math.min(alpha + 0.05f, 1);
        } else {
            alpha = Math.max(alpha - 0.05f, 0);
        }
        if (avatarInfoDisplay != null) {
            avatarInfoDisplay.tick();
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (windowIsActive || isolator.isolatedWidgets.size() == 0 || !this.context.isCompletelyOverlaying()) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(64 / 255f, 64 / 255f, 64 / 255f, alpha);
            context.blit(Screen.BACKGROUND_LOCATION, 0, 0, 0, 0, width, height, 32, 32);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);

            super.render(context, mouseX, mouseY, delta);
            if (avatarInfoDisplay != null) {
                avatarInfoDisplay.render();
            }
        }

        isolator.render(OwoUIDrawContext.of(context), mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        super.removed();
        if (avatarInfoDisplay != null) {
            avatarInfoDisplay.dispose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isolator.onMouseDown(mouseX, mouseY, button))
            return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isolator.onMouseReleased(mouseX, mouseY, button))
            return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        if (isolator.onMouseDragged(mouseX, mouseY, button, dX, dY))
            return true;
        return super.mouseDragged(mouseX, mouseY, button, dX, dY);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void windowActive(boolean focused) {
        windowIsActive = focused;
    }

    @Override
    public boolean testTransparency(Double x, Double y) {
        if (!windowIsActive && isolator.isolatedWidgets.size() != 0) {
            return false;
        }
        if (isolator.hitTest(x, y)) {
            return true;
        }
        if (alpha > 0.2) {
            return true;
        }
        io.wispforest.owo.ui.core.Component hovered = uiAdapter.rootComponent.childAt(x.intValue(), y.intValue());

        if (hovered == null) {
            return false;
        }
        if (hovered.canFocus(io.wispforest.owo.ui.core.Component.FocusSource.MOUSE_CLICK)) {
            return true;
        }

        boolean f = false;

        do {
            if (hovered instanceof Blocker blocker) {
                return blocker.shouldBlock(x, y);
            }
            if (scrapeClassForMouseFocus(hovered.getClass())) {
                f = true;
            }
        } while ((hovered = hovered.parent()) != null);

        return f;
    }
}
