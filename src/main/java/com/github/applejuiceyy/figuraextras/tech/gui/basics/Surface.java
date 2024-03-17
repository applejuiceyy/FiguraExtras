package com.github.applejuiceyy.figuraextras.tech.gui.basics;

import com.github.applejuiceyy.figuraextras.ducks.RenderTargetAccess;
import com.github.applejuiceyy.figuraextras.tech.gui.stack.Stacks;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
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

    static Surface simple(BoundsStackRenderer renderer) {
        return new Surface() {
            @Override
            public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self) {
                Rectangle inner = element.getInnerSpace();
                renderer.render(graphics.pose(), inner.getX(), inner.getY(), inner.getWidth(), inner.getHeight());
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

    static Surface simple(AdvancedElementRenderer renderer) {
        return new Surface() {
            @Override
            public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self) {
                renderer.render(element, graphics, mouseX, mouseY, delta);
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

    @FunctionalInterface
    interface AdvancedElementRenderer {
        void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta);
    }

    @FunctionalInterface
    interface BoundsStackRenderer {
        void render(PoseStack stack, float x, float y, float width, float height);

        default void render(GuiGraphics graphics, float x, float y, float width, float height) {
            render(graphics.pose(), x, y, width, height);
        }
    }

    class Lending<T> implements AutoCloseable {
        private final T thing;
        private final Consumer<T> closer;

        private Lending(T thing, Consumer<T> closer) {
            this.thing = thing;
            this.closer = closer;
        }

        public static <T> Lending<T> lend(T thing, Consumer<T> closing) {
            return new Lending<>(thing, closing);
        }

        @Override
        public void close() {
            closer.accept(thing);
        }
    }
}
