package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.ducks.RenderTargetAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.geometry.Rectangle;
import com.github.applejuiceyy.figuraextras.tech.gui.stack.Stacks;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface Surface {
    Surface EMPTY = new Surface() {
        @Override
        public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self) {

        }

        @Override
        public boolean usesChildren() {
            return false;
        }

        @Override
        public boolean usesSelfRender() {
            return false;
        }
    };

    static Surface contextBackground() {
        return new Surface() {
            @Override
            public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self) {
                Rectangle rectangle = element.getInnerSpace();
                graphics.fill(rectangle.getX(), rectangle.getY(), rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight(), 0xff444444);
                graphics.fill(rectangle.getX() + 1, rectangle.getY() + 1, rectangle.getX() + rectangle.getWidth() - 1, rectangle.getY() + rectangle.getHeight() - 1, 0xff000000);
                graphics.fill(rectangle.getX() + 2, rectangle.getY() + 2, rectangle.getX() + rectangle.getWidth() - 2, rectangle.getY() + rectangle.getHeight() - 2, 0xff111111);
            }

            @Override
            public boolean usesChildren() {
                return false;
            }

            @Override
            public boolean usesSelfRender() {
                return false;
            }
        };
    }

    static Surface solid(int color) {
        return new Surface() {
            @Override
            public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self) {
                Rectangle rectangle = element.getInnerSpace();
                graphics.fill(rectangle.getX(), rectangle.getY(), rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight(), color);
            }

            @Override
            public boolean usesChildren() {
                return false;
            }

            @Override
            public boolean usesSelfRender() {
                return false;
            }
        };
    }

    static Surface text(Supplier<Component> componentSupplier) {
        return new Surface() {
            @Override
            public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self) {
                graphics.drawString(Minecraft.getInstance().font, componentSupplier.get(), element.getX(), element.getY(), 0xffffff, false);
            }

            @Override
            public boolean usesChildren() {
                return false;
            }

            @Override
            public boolean usesSelfRender() {
                return false;
            }
        };
    }

    void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self);

    boolean usesChildren();

    boolean usesSelfRender();

    default Surface mask(Surface mask) {
        Surface self = this;
        return new Surface() {
            @Override
            public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self_) {
                Stacks.RENDER_TARGETS.push();
                mask.render(null, graphics, mouseX, mouseY, delta, children, self_);
                RenderTarget rendered = Stacks.RENDER_TARGETS.pop(false);
                RenderTarget target = Stacks.RENDER_TARGETS.peek();
                ((RenderTargetAccess) target).figuraExtrass$setStencil(rendered.getColorTextureId());
                self.render(null, graphics, mouseX, mouseY, delta, children, self_);
                ((RenderTargetAccess) target).figuraExtrass$setStencil(0);
                Stacks.RENDER_TARGETS.reclaim(rendered);
            }

            @Override
            public boolean usesChildren() {
                return self.usesChildren() || mask.usesChildren();
            }

            @Override
            public boolean usesSelfRender() {
                return self.usesSelfRender() || mask.usesSelfRender();
            }
        };
    }

    default Surface and(Surface other) {
        Surface self = this;
        return new Surface() {
            @Override
            public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self_) {
                self.render(element, graphics, mouseX, mouseY, delta, children, self_);
                other.render(element, graphics, mouseX, mouseY, delta, children, self_);
            }

            @Override
            public boolean usesChildren() {
                return self.usesChildren() || other.usesChildren();
            }

            @Override
            public boolean usesSelfRender() {
                return self.usesSelfRender() || other.usesSelfRender();
            }
        };
    }
}
