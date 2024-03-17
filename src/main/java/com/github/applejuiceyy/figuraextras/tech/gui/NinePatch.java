package com.github.applejuiceyy.figuraextras.tech.gui;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Rectangle;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class NinePatch implements Surface {

    private final boolean tiling;
    private final ResourceLocation resourceLocation;
    private final float regionUVX;
    private final float regionUVY;
    private final float regionUVWidth;
    private final float regionUVHeight;
    private final int leftMargin;
    private final int rightMargin;
    private final int topMargin;
    private final int bottomMargin;
    private final int width;
    private final int height;
    private final float leftUVMargin;
    private final float rightUVMargin;
    private final float topUVMargin;
    private final float bottomUVMargin;

    public NinePatch(boolean tiling, ResourceLocation resourceLocation, float regionUVX, float regionUVY, float regionUVWidth, float regionUVHeight, int width, int height, int leftMargin, int rightMargin, int topMargin, int bottomMargin) {
        this.tiling = tiling;
        this.resourceLocation = resourceLocation;

        this.regionUVX = regionUVX;
        this.regionUVY = regionUVY;
        this.regionUVWidth = regionUVWidth;
        this.regionUVHeight = regionUVHeight;
        this.width = width;
        this.height = height;
        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        this.topMargin = topMargin;
        this.bottomMargin = bottomMargin;
        this.leftUVMargin = ((float) leftMargin / width) * regionUVWidth;
        this.rightUVMargin = ((float) rightMargin / width) * regionUVWidth;
        this.topUVMargin = ((float) topMargin / height) * regionUVHeight;
        this.bottomUVMargin = ((float) bottomMargin / height) * regionUVHeight;
    }

    public void render(PoseStack stack, float x, float y, float width, float height) {
        RenderSystem.setShaderTexture(0, resourceLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = stack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        renderCorners(matrix4f, bufferBuilder, x, y, width, height);
        if (tiling) {
            renderTile(matrix4f, bufferBuilder, x, y, width, height);
        } else {
            renderStretch(matrix4f, bufferBuilder, x, y, width, height);
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private void renderCorners(Matrix4f matrix4f, BufferBuilder bufferBuilder, float x, float y, float width, float height) {
        draw(matrix4f, bufferBuilder, x, x + leftMargin, y, y + topMargin, 0, regionUVX, regionUVX + leftUVMargin, regionUVY, regionUVY + topUVMargin);
        draw(matrix4f, bufferBuilder, x + width - rightMargin, x + width, y, y + topMargin, 0, regionUVX + regionUVWidth - rightUVMargin, regionUVX + regionUVWidth, regionUVY, regionUVY + topUVMargin);
        draw(matrix4f, bufferBuilder, x, x + leftMargin, y + height - bottomMargin, y + height, 0, regionUVX, regionUVX + leftUVMargin, regionUVY + regionUVHeight - bottomUVMargin, regionUVY + regionUVHeight);
        draw(matrix4f, bufferBuilder, x + width - rightMargin, x + width, y + height - bottomMargin, y + height, 0, regionUVX + regionUVWidth - rightUVMargin, regionUVX + regionUVWidth, regionUVY + regionUVHeight - bottomUVMargin, regionUVY + regionUVHeight);
    }

    private void renderStretch(Matrix4f matrix4f, BufferBuilder bufferBuilder, float x, float y, float width, float height) {
        draw(matrix4f, bufferBuilder, x + leftMargin, x + width - rightMargin, y + topMargin, y + height - bottomMargin, 0, regionUVX + leftUVMargin, regionUVX + regionUVWidth - rightUVMargin, regionUVY + topUVMargin, regionUVY + regionUVHeight - bottomUVMargin);

        draw(matrix4f, bufferBuilder, x + leftMargin, x + width - rightMargin, y, y + topMargin, 0, regionUVX + leftUVMargin, regionUVX + regionUVWidth - rightUVMargin, regionUVY, regionUVY + topUVMargin);
        draw(matrix4f, bufferBuilder, x + leftMargin, x + width - rightMargin, y + height - bottomMargin, y + height, 0, regionUVX + leftUVMargin, regionUVX + regionUVWidth - rightUVMargin, regionUVY + regionUVHeight - bottomUVMargin, regionUVY + regionUVHeight);

        draw(matrix4f, bufferBuilder, x, x + leftMargin, y + topMargin, y + height - bottomMargin, 0, regionUVX, regionUVX + leftUVMargin, regionUVY + topUVMargin, regionUVY + regionUVHeight - bottomUVMargin);
        draw(matrix4f, bufferBuilder, x + width - rightMargin, x + width, y + topMargin, y + height - bottomMargin, 0, regionUVX + regionUVWidth - rightUVMargin, regionUVX + regionUVWidth, regionUVY + topUVMargin, regionUVY + regionUVHeight - bottomUVMargin);
    }

    private void renderTile(Matrix4f matrix4f, BufferBuilder bufferBuilder, float x, float y, float width, float height) {
        int spriteCenterWidth = this.width - leftMargin - rightMargin;
        float selectedRegionCenterWidth = width - leftMargin - rightMargin;
        int spriteCenterHeight = this.height - topMargin - bottomMargin;
        float selectedRegionCenterHeight = height - topMargin - bottomMargin;
        for (int tilingX = 0; tilingX < selectedRegionCenterWidth; tilingX += spriteCenterWidth) {
            float spaceX = Math.min(spriteCenterWidth, selectedRegionCenterWidth - tilingX);
            float u2 = regionUVX + leftUVMargin + (regionUVWidth - leftUVMargin - rightUVMargin) * spaceX / spriteCenterWidth;
            draw(matrix4f, bufferBuilder, x + tilingX + leftMargin, x + spaceX + tilingX + leftMargin, y, y + topMargin, 0, regionUVX + leftUVMargin, u2, regionUVY, regionUVY + topUVMargin);

            for (int tilingY = 0; tilingY < selectedRegionCenterHeight; tilingY += spriteCenterHeight) {
                float spaceY = Math.min(spriteCenterHeight, selectedRegionCenterHeight - tilingY);
                draw(matrix4f, bufferBuilder, x + tilingX + leftMargin, x + spaceX + tilingX + leftMargin, y + tilingY + topMargin, y + spaceY + tilingY + topMargin, 0, regionUVX + leftUVMargin, u2, regionUVY + topUVMargin, regionUVY + topUVMargin + (regionUVHeight - topUVMargin - bottomUVMargin) * spaceY / spriteCenterHeight);
            }

            draw(matrix4f, bufferBuilder, x + tilingX + leftMargin, x + spaceX + tilingX + leftMargin, y + height - bottomMargin, y + height, 0, regionUVX + leftUVMargin, u2, regionUVY + regionUVHeight - bottomUVMargin, regionUVY + regionUVHeight);
        }

        for (int tilingY = 0; tilingY < selectedRegionCenterHeight; tilingY += spriteCenterHeight) {
            float spaceY = Math.min(spriteCenterHeight, selectedRegionCenterHeight - tilingY);
            float v2 = regionUVY + topUVMargin + (regionUVHeight - topUVMargin - bottomUVMargin) * spaceY / spriteCenterHeight;
            draw(matrix4f, bufferBuilder, x, x + leftMargin, y + tilingY + topMargin, y + spaceY + tilingY + topMargin, 0, regionUVX, regionUVX + leftUVMargin, regionUVY + topUVMargin, v2);
            draw(matrix4f, bufferBuilder, x + width - rightMargin, x + width, y + tilingY + topMargin, y + spaceY + tilingY + topMargin, 0, regionUVX + regionUVWidth - rightUVMargin, regionUVX + regionUVWidth, regionUVY + topUVMargin, v2);
        }
    }

    private void draw(Matrix4f matrix, BufferBuilder builder, float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2) {
        builder.vertex(matrix, x1, y1, z).uv(u1, v1).endVertex();
        builder.vertex(matrix, x1, y2, z).uv(u1, v2).endVertex();
        builder.vertex(matrix, x2, y2, z).uv(u2, v2).endVertex();
        builder.vertex(matrix, x2, y1, z).uv(u2, v1).endVertex();
    }

    @Override
    public void render(Element element, GuiGraphics graphics, int mouseX, int mouseY, float delta, @Nullable Runnable children, @Nullable Runnable self) {
        Rectangle rectangle = element.getInnerSpace();
        render(graphics.pose(), rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
    }

    @Override
    public boolean usesChildren() {
        return false;
    }

    @Override
    public boolean usesSelfRender() {
        return false;
    }
}
