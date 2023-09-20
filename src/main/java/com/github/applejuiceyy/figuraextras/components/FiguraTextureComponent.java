package com.github.applejuiceyy.figuraextras.components;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.util.ScissorStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;

import java.util.function.Supplier;

public class FiguraTextureComponent extends BaseComponent {
    private final FiguraTexture texture;
    private final Supplier<ResourceLocation> location;
    private final Avatar avatar;

    public FiguraTextureComponent(FiguraTexture texture, Supplier<ResourceLocation> location, Avatar avatar) {
        this.texture = texture;
        this.location = location;
        this.avatar = avatar;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return this.texture.getHeight();
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return this.texture.getWidth();
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        var matrices = context.pose();
        ScissorStack.push(x, y, width, height, matrices);
        boolean pop = false;
        if (texture.getWidth() <= width) {
            pop = true;
            ScissorStack.push(
                    x + width / 2 - texture.getWidth() / 2,
                    y,
                    texture.getWidth(),
                    height,
                    matrices
            );
        }
        RenderSystem.clearColor(0, 0, 0, 0);
        RenderSystem.clear(GlConst.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        if (pop) {
            ScissorStack.pop();
        }
        matrices.pushPose();

        boolean shouldDrawRuler = false;
        if (texture.getWidth() <= width || !isInBoundingBox(mouseX, mouseY)) {
            matrices.translate(x + (float) width / 2 - (float) texture.getWidth() / 2, y, 0);
        } else {
            float percentage = (float) (mouseX - x) / width;
            int overflownAmount = texture.getWidth() - width;
            matrices.translate(x + (-overflownAmount * percentage), y, 0);

            shouldDrawRuler = true;
        }

        context.blit(this.location.get(), 0, 0, 0, 0, texture.getWidth(), texture.getHeight(), texture.getWidth(), texture.getHeight());

        if (shouldDrawRuler) {
            int steps = ((width / 10) / 50) * 50;
            steps = steps <= 0 ? 10 : steps;
            for (int i = steps; i < texture.getWidth(); i += steps) {
                context.drawText(Component.literal(String.valueOf(i)), i + 1, 1, 0.4f, 0xaaffffff);
                context.fill(i - 1, 0, i, 5, 0xaaffffff);
            }
        }

        matrices.popPose();
        ScissorStack.pop();
    }
}
